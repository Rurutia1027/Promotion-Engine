package com.tus.coupon.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tus.coupon.common.constants.DistributionRocketMQConstant;
import com.tus.coupon.common.dao.entity.CouponTaskDO;
import com.tus.coupon.common.dao.entity.CouponTaskFailDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.common.dao.mapper.CouponTaskMapper;
import com.tus.coupon.common.dao.mapper.CouponTemplateMapper;
import com.tus.coupon.common.dao.mapper.UserCouponMapper;
import com.tus.coupon.common.enums.CouponTaskStatusEnum;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.distribution.common.DistributionRedisConstant;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import com.tus.coupon.distribution.service.handler.UserCouponTaskFailExcelObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private void decrementCouponTemplateStockAndSaveUserCouponList(CouponTemplateDistributionEvent event) {
    }

    private List<CouponTaskFailDO> listUserCouponTaskFail(Long couponTaskBatchId, long initId) {
        // todo 
        return null;
    }
}
