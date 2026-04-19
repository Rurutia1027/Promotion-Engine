package com.tus.coupon.distribution.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.tus.coupon.common.constants.DistributionRocketMQConstant;
import com.tus.coupon.common.mq.base.BaseSendExtendDTO;
import com.tus.coupon.common.mq.base.MessageWrapper;
import com.tus.coupon.distribution.mq.event.CouponTemplateDistributionEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CouponExecuteDistributionProducer extends AbstractCommonSendProduceTemplate<CouponTemplateDistributionEvent> {
    private final ConfigurableEnvironment env;

    public CouponExecuteDistributionProducer(@Autowired RocketMQTemplate rocketMQTemplate,
                                             @Autowired ConfigurableEnvironment env) {
        super(rocketMQTemplate);
        this.env = env;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTemplateDistributionEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("Coupon Delivery Event")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic(env.resolvePlaceholders(DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    // subclass customized two part of message {msg payload , msg extension
    // parameters/metadata} wrap together
    @Override
    protected Message<?> buildMessage(CouponTemplateDistributionEvent event,
                                      BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ?
                UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, event))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
