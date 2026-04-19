package com.tus.coupon.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tus.coupon.common.constants.DistributionRocketMQConstant;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.entity.UserCouponDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.common.dao.mapper.CouponTaskMapper;
import com.tus.coupon.common.dao.mapper.CouponTemplateMapper;
import com.tus.coupon.common.dao.mapper.UserCouponMapper;
import com.tus.coupon.common.enums.CouponSourceEnum;
import com.tus.coupon.common.enums.CouponStatusEnum;
import com.tus.coupon.common.enums.CouponTaskStatusEnum;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.distribution.common.DistributionRedisConstant;
import com.tus.coupon.distribution.common.EngineRedisConstant;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.tus.coupon.distribution.service.handler.UserCouponTaskFailExcelObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchExecutorException;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.tus.coupon.distribution.common.EngineRedisConstant.USER_COUPON_TEMPLATE_LIMIT_KEY;

// dispatch coupon to specific user and update user records

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY,
        consumerGroup = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_CG_KEY
)
@Slf4j(topic = "CouponExecuteDistributionConsumer")
public class CouponExecuteDistributionConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDistributionEvent>> {
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponTaskMapper couponTaskMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Autowired
    private CouponExecuteDistributionConsumer couponExecuteDistributionConsumer;

    private final static int BATCH_USER_COUPON_SIZE = 5000;
    private static final String BATCH_SAVE_USER_COUPON_LUA_PATH = "lua" +
            "/batch_user_coupon_list.lua";
    private final String excelPath = Paths.get("").toAbsolutePath() + "/tmp";

    @Autowired
    private CouponTaskFailMapper couponTaskFailMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<CouponTemplateDistributionEvent> messageWrapper) {
        // when received event's batch user set size attach batch process threshold
        // or
        // event contains coupon distribution end flag = true --> the last batch
        CouponTemplateDistributionEvent event = messageWrapper.getMessage();
        if (!event.getDistributionEndFlag() && event.getBatchUserSetSize() % BATCH_USER_COUPON_SIZE == 0) {
            decrementCouponTemplateStockAndSaveUserCouponList(event);
        }

        // if received event's distribution end flag = true, also handle coming event
        if (event.getDistributionEndFlag()) {
            // first we fetch redis batch user set key
            String batchUserSetKey =
                    String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());
            Long batchUserIdsSize = stringRedisTemplate.opsForSet().size(batchUserSetKey);
            event.setBatchUserSetSize(batchUserIdsSize.intValue());
            decrementCouponTemplateStockAndSaveUserCouponList(event);

            // here we fetch batch of user ids in set/list from redis cache
            List<String> batchUserMaps = stringRedisTemplate.opsForSet().pop(batchUserSetKey,
                    Integer.MAX_VALUE);
            if (CollUtil.isNotEmpty(batchUserMaps)) {
                // coupon stock out of usage caused user's corresponding batch user key
                // still remains in redis set
                List<CouponTaskFailDO> couponTaskFailDOList =
                        new ArrayList<>(batchUserMaps.size());
                for (String batchUserMapStr : batchUserMaps) {
                    Map<Object, Object> objectMap = MapUtil.builder()
                            .put("rowNum", JSON.parseObject(batchUserMapStr).get("rowNum"))
                            .put("cause", "coupon template stock run out of usage")
                            .build();
                    CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                            .batchId(event.getCouponTaskBatchId())
                            .jsonObject(JSON.toJSONString(objectMap))
                            .build();
                    couponTaskFailDOList.add(couponTaskFailDO);
                }

                // commit list of coupon failed task do to db
                couponTaskFailMapper.insert(couponTaskFailDOList);
            }

            long initId = 0;
            boolean isFirstIteration = true; // first round iteration
            String failFileAddress =
                    excelPath + "/user_coupon_delivery_failure-Excel" + event.getCouponTaskBatchId() + ".xlsx";

            // todo: refactor logic here upload excel file to cloud storage
            try (ExcelWriter excelWriter = EasyExcel.write(failFileAddress,
                    UserCouponTaskFailExcelObject.class).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet("User coupon delivery " +
                        "failure Sheet").build();
                while (true) {
                    List<CouponTaskFailDO> couponTaskFailDOList =
                            listUserCouponTaskFail(event.getCouponTaskBatchId(), initId);
                    if (CollUtil.isEmpty(couponTaskFailDOList)) {
                        // if first round iteration with set empty, set failFileAddress to
                        // null
                        if (isFirstIteration) {
                            failFileAddress = null;
                        }
                        break;
                    }

                    // here remark iteration round to non-first round
                    isFirstIteration = false;

                    // here coupon task fail do list is non-empty, iterate each DO and
                    // convert into UserCouponFailExcelObject and via writer write fail
                    // excel obj to Excel file
                    List<UserCouponTaskFailExcelObject> excelFailObjList =
                            couponTaskFailDOList.stream().map(each -> JSONObject.parseObject(each.getJsonObject(),
                                            UserCouponTaskFailExcelObject.class))
                                    .toList();
                    excelWriter.write(excelFailObjList, writeSheet);

                    // if queried list size < batch threshold value break pending to next
                    // round
                    if (couponTaskFailDOList.size() < BATCH_USER_COUPON_SIZE) {
                        break;
                    }

                    // refresh initId to max ID value
                    initId = couponTaskFailDOList.stream().mapToLong(CouponTaskFailDO::getId)
                            .max().orElse(initId);
                } // while end
            }

            // when we got here, it means all users receive coupon, then we can update the
            // coupon task status and complete time
            CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                    .id(event.getCouponTaskId())
                    .status(CouponTaskStatusEnum.SUCCESS.getStatus())
                    .failFileAddress(failFileAddress)
                    .completionTime(new Date())
                    .build();

            couponTaskMapper.updateById(couponTaskDO);
        }
    }

    @SneakyThrows
    private void decrementCouponTemplateStockAndSaveUserCouponList(CouponTemplateDistributionEvent event) {
        // if ret value <= 0 means coupon stock out of usage , return
        Integer couponTemplateStock = decrementCouponTemplateStock(event,
                event.getBatchUserSetSize());
        if (couponTemplateStock <= 0) {
            return;
        }

        // fetch batch user set from redis cache
        String batchUserSetKey =
                String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY,
                        event.getCouponTaskId());
        List<String> batchUserMaps = stringRedisTemplate.opsForSet().pop(batchUserSetKey,
                couponTemplateStock);

        List<UserCouponDO> userCouponDOList = new ArrayList<>(batchUserMaps.size());
        Date now = new Date();

        // construct user coupon list via the set of user info that are fetch from redis cache
        for (String each : batchUserMaps) {
            JSONObject userIdAndRowNumJsonObj = JSON.parseObject(each);
            DateTime validEndTime = DateUtil.offsetHour(now,
                    JSON.parseObject(event.getCouponTemplateConsumeRule()).getInteger(
                            "validityPeriod"));
            UserCouponDO userCouponDO = UserCouponDO.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .couponTemplateId(event.getCouponTemplateId())
                    .rowNum(userIdAndRowNumJsonObj.getInteger("rowNum"))
                    .userId(userIdAndRowNumJsonObj.getLong("userId"))
                    .receiveTime(now)
                    .receiveCount(1) // first time to fetch coupon
                    .validStartTime(now)
                    .source(CouponSourceEnum.PLATFORM.getType())
                    .status(CouponStatusEnum.EFFECTIVE.getType())
                    .createTime(new Date())
                    .updateTime(new Date())
                    .delFlag(0)
                    .build();

            userCouponDOList.add(userCouponDO);
        }

        // platform coupon each user only allowed to retrieve once
        batchSaveUserCouponList(event.getCouponTemplateId(), event.getCouponTaskBatchId(),
                userCouponDOList);

        // update all the current user coupon do list to the user who received coupons records
        List<String> userIdList = userCouponDOList.stream()
                .map(UserCouponDO::getUserId)
                .map(String::valueOf)
                .toList();

        String userIdsJson = new ObjectMapper().writeValueAsString(userIdList);
        List<String> couponIdList = userCouponDOList.stream()
                .map(each -> StrUtil.builder()
                        .append(event.getCouponTemplateId())
                        .append("_")
                        .append(each.getId())
                        .toString())
                .map(String::valueOf)
                .toList();

        String couponIdsJson = new ObjectMapper().writeValueAsString(couponIdList);

        // here we invoke lua script and passing context wrapped variables
        List<String> keys = Arrays.asList(
                StrUtil.replace(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, "%s", ""),
                USER_COUPON_TEMPLATE_LIMIT_KEY,
                String.valueOf(event.getCouponTemplateId())
        );

        List<String> args = Arrays.asList(
                userIdsJson,
                couponIdsJson,
                String.valueOf(new Date().getTime()),
                String.valueOf(
                        Duration.between(
                                LocalDateTime.now(),
                                event.getValidEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        ).getSeconds()
                )
        );
        DefaultRedisScript<Void> buildLuaScript = Singleton.get(BATCH_SAVE_USER_COUPON_LUA_PATH,
                () -> {
                    DefaultRedisScript<Void> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(BATCH_SAVE_USER_COUPON_LUA_PATH)));
                    redisScript.setResultType(Void.class);
                    return redisScript;
                });
        stringRedisTemplate.execute(buildLuaScript, keys, args.toArray());

        // here we add increase stock roll back solution, suppose user already receive
        // coupon and validated, service need rollback the stock
        int originalUserCouponSize = batchUserMaps.size();
        int availableUserCouponSize = userCouponDOList.size();
        int rollbackStock = originalUserCouponSize - availableUserCouponSize;
        if (rollbackStock > 0) {
            stringRedisTemplate.opsForHash().increment(
                    String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, event.getCouponTemplateId()),
                    "stock",
                    rollbackStock
            );

            couponTemplateMapper.increaseNumberCouponTemplate(event.getShopNumber(),
                    event.getCouponTemplateId(), rollbackStock);
        }
    }

    private void batchSaveUserCouponList(Long couponTemplateId, Long couponTaskBatchId, List<UserCouponDO> userCouponDOList) {
        try {
            userCouponMapper.insert(userCouponDOList, userCouponDOList.size());
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BatchExecutorException) {
                // record the user coupon distribution failure to db table
                List<CouponTaskFailDO> couponTaskFailDOList = new ArrayList<>();
                List<UserCouponDO> toRemove = new ArrayList<>();

                // invoke batch insert failure, to avoid large scale of retry failure, we
                // insert records to db table one by one
                userCouponDOList.forEach(each -> {
                    try {
                        userCouponMapper.insert(each);
                    } catch (Exception ignored) {
                        Boolean hasReceived =
                                couponExecuteDistributionConsumer.hasUserReceivedCoupon(couponTemplateId, each.getUserId());
                        if (hasReceived) {
                            // generate coupon task fail do entity and append to fail do list
                            Map<Object, Object> objectMap = MapUtil.builder()
                                    .put("rowNum", each.getRowNum())
                                    .put("cause", "user has already received coupon before")
                                    .build();
                            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                                    .batchId(couponTaskBatchId)
                                    .jsonObject(JSON.toJSONString(objectMap))
                                    .build();
                            couponTaskFailDOList.add(couponTaskFailDO);

                            // remove that from user coupon don list
                            toRemove.add(each);
                        }
                    }
                });

                // batch update t_coupon_task_fail table
                couponTaskFailMapper.insert(couponTaskFailDOList,
                        couponTaskFailDOList.size());

                // remove duplicate user info item
                userCouponDOList.removeAll(toRemove);
                return;
            }
            throw ex;
        }
    }

    private Integer decrementCouponTemplateStock(CouponTemplateDistributionEvent event,
                                                 Integer decrementStockSize) {
        Long couponTemplateId = event.getCouponTemplateId();
        int decremented =
                couponTemplateMapper.decrementCouponTemplateStock(event.getShopNumber(),
                        couponTemplateId, decrementStockSize);

        // if db modify request failed, it means coupon stock out of usage, requires retry
        if (!SqlHelper.retBool(decremented)) {
            LambdaQueryWrapper<CouponTemplateDO> queryWrapper =
                    Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getShopNumber, event.getShopNumber())
                            .eq(CouponTemplateDO::getId, couponTemplateId);
            CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
            return decrementCouponTemplateStock(event, couponTemplateDO.getStock());
        }

        return decrementStockSize;
    }

    private List<CouponTaskFailDO> listUserCouponTaskFail(Long couponTaskBatchId, long initId) {
        // todo 
        return null;
    }

    /**
     * Validate whether that user has already received the coupon before
     *
     * @param couponTemplateId coupon template id
     * @param userId
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Boolean hasUserReceivedCoupon(Long couponTemplateId, Long userId) {
        LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getCouponTemplateId, couponTemplateId);
        return userCouponMapper.selectOne(queryWrapper) != null;
    }

}
