package com.tus.coupon.merchant.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.tus.coupon.common.mq.base.BaseSendExtendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

// RocketMQ producer basic operation abstract clas
@RequiredArgsConstructor
@Slf4j(topic = "CommonSendProduceTemplate")
public abstract class AbstractCommonSendProduceTemplate<T> {
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Deliver message constructor
     *
     * @param messageSendEvent
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * Deliver message basic params, header, keys, payload
     *
     * @param messageSendEvent delivery message event
     * @param requestParam     extension entities
     */
    protected abstract Message<?> buildMessage(T messageSendEvent,
                                               BaseSendExtendDTO requestParam);

    /***
     * Common entry point for message delivery
     *
     * @param messageSendEvent message delivery event
     * @return message event delivery response result
     */
    public SendResult sendMessage(T messageSendEvent) {
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;

        try {
            StringBuilder destinationBuilder =
                    StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // if delay time not null, then delivery message in delay mode,
            // other delivery message in normal mode
            if (baseSendExtendDTO.getDelayTime() != null) {
                // delay mode message delivery
                sendResult = rocketMQTemplate.syncSendDeliverTimeMills(
                        destinationBuilder.toString(),
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getDelayTime()
                );
            } else {
                // normal mode message delivery
                sendResult = rocketMQTemplate.syncSend(
                        destinationBuilder.toString(),
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getSentTimeout()
                );
            }
        } catch (Throwable ex) {
            throw ex;
        }

        return sendResult;
    }
}
