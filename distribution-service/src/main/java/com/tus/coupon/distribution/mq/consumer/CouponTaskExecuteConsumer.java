package com.tus.coupon.distribution.mq.consumer;

import com.tus.coupon.common.mq.base.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "",
        consumerGroup = ""
)
@Slf4j(topic = "CouponTaskExecuteConsumer")
public class CouponTaskExecuteConsumer implements RocketMQListener<MessageWrapper> {
    @Override
    public void onMessage(MessageWrapper messageWrapper) {

    }
}
