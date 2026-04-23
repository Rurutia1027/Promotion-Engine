package com.tus.coupon.distribution.mq.consumer;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.dao.mapper.CouponTaskFailMapper;
import com.tus.coupon.common.dao.mapper.CouponTaskMapper;
import com.tus.coupon.common.dao.mapper.CouponTemplateMapper;
import com.tus.coupon.common.enums.CouponTaskStatusEnum;
import com.tus.coupon.common.enums.CouponTemplateStatusEnum;
import com.tus.coupon.common.idempotent.NoMQDuplicateConsume;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.common.mq.event.CouponTaskExecuteEvent;
import com.tus.coupon.distribution.mq.producer.CouponExecuteDistributionProducer;
import com.tus.coupon.distribution.service.handler.CouponTaskExcelObject;
import com.tus.coupon.distribution.service.handler.CouponTaskUserPageHandler;
import com.tus.coupon.distribution.service.handler.ReadExcelDistributionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.tus.coupon.common.mq.constant.MerchantDistributeCouponsRocketMQConstant.TEMPLATE_TASK_EXECUTE_CG_KEY;
import static com.tus.coupon.common.mq.constant.MerchantDistributeCouponsRocketMQConstant.TEMPLATE_TASK_EXECUTE_TOPIC_KEY;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TEMPLATE_TASK_EXECUTE_TOPIC_KEY,
        consumerGroup = TEMPLATE_TASK_EXECUTE_CG_KEY
)
@Slf4j(topic = "CouponTaskExecuteConsumer")
public class CouponTaskExecuteConsumer implements RocketMQListener<MessageWrapper<CouponTaskExecuteEvent>> {
    private final CouponTaskMapper couponTaskMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponTaskUserPageHandler couponTaskUserPageHandler;


    @NoMQDuplicateConsume(
            keyPrefix = "coupon_task_execute:idempotent",
            key = "#messageWrapper.message.couponTaskId",
            keyTimeout = 120
    )
    @Override
    public void onMessage(MessageWrapper<CouponTaskExecuteEvent> messageWrapper) {
        var couponTaskId = messageWrapper.getMessage().getCouponTaskId();
        var couponTaskDO = couponTaskMapper.selectById(couponTaskId);

        if (ObjectUtil.notEqual(couponTaskDO.getStatus(),
                CouponTaskStatusEnum.IN_PROGRESS.getStatus())) {
            // coupon delivery task already in progress, return
            return;
        }

        // validate whether coupon task status is as expected
        var queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getId, couponTaskDO.getCouponTemplateId())
                .eq(CouponTemplateDO::getShopNumber, couponTaskDO.getShopNumber());
        var couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
        var status = couponTemplateDO.getStatus();
        if (ObjectUtil.notEqual(status, CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            // coupon's template got expired, coupon cannot be used anymore
            return;
        }

        // begin deliver coupons by paging user directly from db
        couponTaskUserPageHandler.executeByUserPages(couponTaskDO, couponTemplateDO);
    }

}
