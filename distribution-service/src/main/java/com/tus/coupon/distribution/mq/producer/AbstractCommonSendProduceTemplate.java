package com.tus.coupon.distribution.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.tus.coupon.common.mq.base.BaseSendExtendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
@Slf4j(topic = "CommonSendProduceTemplate")
public abstract class AbstractCommonSendProduceTemplate<T> {
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Construct message payloads
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * Combine gonna delivery message payload + extension parameters together.
     * Construct message parameters, request header, keys
     *
     * @param messageSendEvent message payload
     * @param requestParam     extension parameters
     */
    protected abstract Message<?> buildMessage(T messageSendEvent,
                                               BaseSendExtendDTO requestParam);

    /**
     * delivery message
     *
     * @param messageSendEvent delivery message payload
     */
    public SendResult sendMessage(T messageSendEvent) {
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            // construct Topic target formats: `topicName:tags`
            StringBuilder destinationBuilder =
                    StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // if delay time field not empty, then delivery RocketMQ delay type message,
            // otherwise delivery normal message
            if (baseSendExtendDTO.getDelayTime() != null) {
                sendResult = rocketMQTemplate.syncSendDeliverTimeMills(
                        destinationBuilder.toString(),
                        // combination message payload + message header/keys metadata via
                        // function buildMessage here
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getDelayTime()
                );
            } else {
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
