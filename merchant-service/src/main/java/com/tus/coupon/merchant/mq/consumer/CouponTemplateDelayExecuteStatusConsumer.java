package com.tus.coupon.merchant.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tus.coupon.common.constants.MerchantRocketMQConstant;
import com.tus.coupon.common.dao.entity.CouponTemplateDO;
import com.tus.coupon.common.enums.CouponTemplateStatusEnum;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.common.mq.event.CouponTemplateDelayEvent;
import com.tus.coupon.merchant.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MerchantRocketMQConstant.TEMPLATE_DELAY_TOPIC_KEY,
        consumerGroup = MerchantRocketMQConstant.TEMPLATE_DELAY_STATUS_CG_KEY
)
@Slf4j(topic = "CouponTemplateDelayExecuteStatusConsumer")
public class CouponTemplateDelayExecuteStatusConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDelayEvent>> {
    private final CouponTemplateService couponTemplateService;

    @Override
    public void onMessage(MessageWrapper<CouponTemplateDelayEvent> messageWrapper) {
        CouponTemplateDelayEvent message = messageWrapper.getMessage();
        LambdaUpdateWrapper<CouponTemplateDO> updateWrapper = Wrappers.lambdaUpdate(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, message.getShopNumber())
                .eq(CouponTemplateDO::getId, message.getCouponTemplateId());
        CouponTemplateDO couponTemplateDO = CouponTemplateDO.builder()
                .status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build();
        couponTemplateService.update(couponTemplateDO, updateWrapper);
    }
}
