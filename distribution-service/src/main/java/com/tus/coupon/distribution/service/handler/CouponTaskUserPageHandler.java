package com.tus.coupon.distribution.service.handler;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.distribution.common.DistributionRedisConstant;
import com.tus.coupon.distribution.common.EngineRedisConstant;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.tus.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.tus.coupon.distribution.service.UserRemoteQueryService;
import com.tus.coupon.distribution.tookit.StockDecrementReturnCombinedUtil;
import com.tus.coupon.user.api.dto.resp.UserItemRespDTO;
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

    private final UserRemoteQueryService userRemoteQueryService;
    private final CouponTaskFailMapper couponTaskFailMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CouponExecuteDistributionProducer couponExecuteDistributionProducer;

    @Value("${distribution.task.page-size:" + DEFAULT_PAGE_SIZE + "}")
    private int pageSize;

    @Value("${distribution.task.batch-threshold:" + DEFAULT_BATCH_USER_COUPON_SIZE + "}")
    private int batchThreshold;

    public void executeByUserPages(CouponTaskDO couponTaskDO, CouponTemplateDO couponTemplateDO) {
        Long couponTaskId = couponTaskDO.getId();
        String cursorKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_CURSOR_KEY, couponTaskId);
        long cursor = readCursor(cursorKey);
        int rowNum = 1;
        int processedCount = 0;
        int pageCount = 0;

        while (true) {
            List<UserItemRespDTO> users = listTaskUsers(couponTaskDO, cursor, pageSize);
            if (users.isEmpty()) {
                break;
            }
            pageCount++;

            for (UserItemRespDTO user : users) {
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

    private List<UserItemRespDTO> listTaskUsers(CouponTaskDO couponTaskDO, Long cursor, int limit) {
        return userRemoteQueryService.queryMerchantUsers(
                String.valueOf(couponTaskDO.getShopNumber()),
                cursor,
                limit
        );
    }

    private int processSingleUser(CouponTaskDO couponTaskDO,
                                  CouponTemplateDO couponTemplateDO,
                                  UserItemRespDTO user,
                                  int rowNum,
                                  String cursorKey) {
        Long couponTaskId = couponTaskDO.getId();

        DefaultRedisScript<Long> buildLuaScript =
                Singleton.get(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH, () -> {
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(
                            new ClassPathResource(STOCK_DECREMENT_AND_BATCH_SAVE_USER_RECORD_LUA_PATH)));
                    redisScript.setResultType(Long.class);
                    return redisScript;
                });

        String couponTemplateKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, couponTaskId);

        Map<Object, Object> userRowNumMap = MapUtil.builder()
                .put("userId", String.valueOf(user.getId()))
                .put("rowNum", rowNum + 1)
                .build();

        Long combinedField = stringRedisTemplate.execute(
                buildLuaScript,
                ListUtil.of(couponTemplateKey, batchUserSetKey),
                JSON.toJSONString(userRowNumMap));

        boolean firstField = StockDecrementReturnCombinedUtil.extractFirstField(combinedField);
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

        int batchUserSetSize = StockDecrementReturnCombinedUtil.extractSecondField(combinedField.intValue());
        if (batchUserSetSize < batchThreshold && StrUtil.isBlank(couponTaskDO.getNotifyType())) {
            stringRedisTemplate.opsForValue().set(cursorKey, String.valueOf(user.getId()));
            return rowNum + 1;
        }

        CouponTemplateDistributionEvent couponTemplateDistributionEvent =
                CouponTemplateDistributionEvent.builder()
                        .userId(String.valueOf(user.getId()))
                        .mail(user.getMail())
                        .phone(user.getPhone())
                        .couponTaskId(couponTaskId)
                        .notifyType(couponTaskDO.getNotifyType())
                        .shopNumber(couponTaskDO.getShopNumber())
                        .couponTemplateId(couponTemplateDO.getId())
                        .validEndTime(couponTemplateDO.getValidEndTime())
                        .couponTaskBatchId(couponTaskDO.getBatchId())
                        .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                        .batchUserSetSize(batchUserSetSize)
                        .distributionEndFlag(Boolean.FALSE)
                        .build();

        couponExecuteDistributionProducer.sendMessage(couponTemplateDistributionEvent);
        stringRedisTemplate.opsForValue().set(cursorKey, String.valueOf(user.getId()));
        return rowNum + 1;
    }

    private void sendFinalizeEvent(CouponTaskDO couponTaskDO, CouponTemplateDO couponTemplateDO) {
        CouponTemplateDistributionEvent couponTemplateExecuteEvent = CouponTemplateDistributionEvent.builder()
                .distributionEndFlag(Boolean.TRUE)
                .shopNumber(couponTaskDO.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .validEndTime(couponTemplateDO.getValidEndTime())
                .couponTemplateConsumeRule(couponTemplateDO.getConsumeRule())
                .couponTaskBatchId(couponTaskDO.getBatchId())
                .couponTaskId(couponTaskDO.getId())
                .build();
        couponExecuteDistributionProducer.sendMessage(couponTemplateExecuteEvent);
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