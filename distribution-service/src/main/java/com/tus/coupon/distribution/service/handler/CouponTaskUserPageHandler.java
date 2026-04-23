package com.tus.coupon.distribution.service.handler;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.entity.UserDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.common.dao.mapper.UserMapper;
import com.tus.coupon.distribution.common.DistributionRedisConstant;
import com.tus.coupon.distribution.common.EngineRedisConstant;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.tus.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.tus.coupon.distribution.tookit.StockDecrementReturnCombinedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTaskUserPageHandler {
    private static final String STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH =
            "lua/stock_decrement_and_batch_save_user_record.lua";
    private static final int DEFAULT_PAGE_SIZE = 500;
    private static final int DEFAULT_BATCH_USER_COUPON_SIZE = 5000;

    private final UserMapper userMapper;
    private final CouponTaskFailMapper couponTaskFailMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CouponExecuteDistributionProducer couponExecuteDistributionProducer;

    @org.springframework.beans.factory.annotation.Value("${distribution.task.page-size:" + DEFAULT_PAGE_SIZE + "}")
    private int pageSize;

    @Value("${distribution.task.batch-threshold:" + DEFAULT_BATCH_USER_COUPON_SIZE + "}")
    private int batchThreshold;

    public void executeByUserPages(CouponTaskDO couponTaskDO,
                                   CouponTemplateDO couponTemplateDO) {
        Long couponTaskId = couponTaskDO.getId();
        String cursorKey =
                String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_CURSOR_KEY,
                        couponTaskId);
        long cursor = readCursor(cursorKey);
        int rowNum = 1;
        int processedCount = 0;
        int pageCount = 0;

        while (true) {
            List<UserDO> users = listTaskUsers(couponTaskDO, cursor, pageSize);
            if (users.isEmpty()) {
                break;
            }
            pageCount++;

            for (UserDO user : users) {
                rowNum = processSingleUser(couponTaskDO, couponTemplateDO, user, rowNum, cursorKey);
                cursor = user.getId();
                processedCount++;
            }

            stringRedisTemplate.opsForValue().set(cursorKey, String.valueOf(cursor));
            log.info("coupon task page processed. taskId={}, pageCount={}, processedCount={}, cursor={}, pageSize={}",
                    couponTaskId, pageCount, processedCount, cursor, pageSize);
        }

        sendFinalizeEvent(couponTaskDO, couponTemplateDO);
        log.info("coupon task paging done. taskId={}, totalPages={}, totalProcessed={}, finalCursor={}",
                couponTaskId, pageCount, processedCount, cursor);
    }

    private int processSingleUser(CouponTaskDO couponTaskDO, CouponTemplateDO couponTemplateDO,
                                  UserDO user, int rowNum, String cursorKey) {
        Long couponTaskId = couponTaskDO.getId();
        DefaultRedisScript<Long> buildLuaScript =
                Singleton.get(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH, () -> {
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(
                            new ClassPathResource(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH)));
                    redisScript.setResultType(Long.class);
                    return redisScript;
                });

        String couponTempalteKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY,
                couponTemplateDO.getId());
        String batchUserSetKey =
                String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY
                        , couponTaskId);

        Map<Object, Object> userRowNumMap = MapUtil.builder()
                .put("userId", String.valueOf(user.getId()))
                .put("rowNum", rowNum + 1)
                .build();

        Long combinedField = stringRedisTemplate.execute(
                buildLuaScript,
                ListUtil.of(couponTempalteKey, batchUserSetKey),
                JSON.toJSONString(userRowNumMap));

        boolean firstField =
                StockDecrementReturnCombinedUtil.extractFirstField(combinedField);
        if (!firstField) {
            stringRedisTemplate.opsForValue().set(cursorKey, String.valueOf(user.getId()));
            int currentRowNum = rowNum + 1;
            Map<Object, Object> objectMap = MapUtil.builder()
                    .put("rowNum", currentRowNum)
                    .put("cause", "coupon run out of usage")
                    .build();
            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                    .batchId(couponTaskDO.getBatchId())
                    .jsonObject(JSON.toJSONString(objectMap, SerializerFeature.WriteMapNullValue))
                    .build();
            couponTaskFailMapper.insert(couponTaskFailDO);
            return currentRowNum;
        }

        return 0;
    }

    private List<UserDO> listTaskUsers(CouponTaskDO couponTaskDO, long cursor, int limit) {
        return userMapper.selectList(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getShopNumber, String.valueOf(couponTaskDO.getShopNumber()))
                .eq(UserDO::getDelFlag, 0)
                .gt(UserDO::getId, cursor)
                .orderByAsc(UserDO::getId)
                .last("LIMIT " + limit));
    }

    // take consideration of WAL for better durability and failure recovery ability
    private void sendFinalizeEvent(CouponTaskDO couponTaskDO,
                                   CouponTemplateDO couponTemplateDO) {
        CouponTemplateDistributionEvent couponTemplateDistributionEvent =
                CouponTemplateDistributionEvent.builder()
                        .distributionEndFlag(Boolean.TRUE)
                        .shopNumber(couponTaskDO.getShopNumber())
                        .couponTemplateId(couponTemplateDO.getId())
                        .validEndTime(couponTemplateDO.getValidEndTime())
                        .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                        // here 1 task : N batch
                        .couponTaskBatchId(couponTaskDO.getBatchId())
                        .couponTaskId(couponTaskDO.getId())
                        .build();
        couponExecuteDistributionProducer.sendMessage(couponTemplateDistributionEvent);
    }

    private long readCursor(String cursorKey) {
        String cursor = stringRedisTemplate.opsForValue().get(cursorKey);
        if (StrUtil.isBlank(cursor)) {
            return 0L;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            log.warn("invalid task cursor value, fallback to 0. cursorKey={}, cursor={}", cursorKey, cursor);
            return 0L;
        }
    }
}
