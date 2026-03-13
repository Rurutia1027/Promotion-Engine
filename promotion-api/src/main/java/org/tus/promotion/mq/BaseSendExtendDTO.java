package org.tus.promotion.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message event based extension DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class BaseSendExtendDTO {

    /**
     * event name
     */
    private String eventName;

    /**
     * topic
     */
    private String topic;

    /**
     * tag
     */
    private String tag;

    /**
     * message key
     */
    private String keys;

    /**
     * send message timeout in milliseconds (MS)
     */
    private Long sentTimeout;

    /**
     * Precise delay time (milliseconds), suitable for scenarios scheduled based on
     * timestamps or milliseconds-level timing.
     */
    private Long delayTime;

    /**
     * RocketMQ delay level, suitable for scenarios that use the broker's predefined delay
     * levels.
     */
    private Integer delayLevel;
}