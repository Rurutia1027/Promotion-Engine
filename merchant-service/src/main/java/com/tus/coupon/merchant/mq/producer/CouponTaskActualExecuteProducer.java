package com.tus.coupon.merchant.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.tus.coupon.common.mq.base.BaseSendExtendDTO;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.merchant.common.constant.MerchantAdminRocketMQConstant;
import com.tus.coupon.merchant.mq.event.CouponTaskExecuteEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

// coupon delivery task executor side's producer
@Slf4j
@Component
public class CouponTaskActualExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTaskExecuteEvent> {
    private final ConfigurableEnvironment env;

    public CouponTaskActualExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate,
                                           @Autowired ConfigurableEnvironment env) {
        super(rocketMQTemplate);
        this.env = env;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTaskExecuteEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("Coupon Delivery Task")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic(env.resolvePlaceholders(MerchantAdminRocketMQConstant.TEMPLATE_TASK_EXECUTE_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTaskExecuteEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ?
                UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
