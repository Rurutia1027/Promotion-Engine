package com.tus.coupon.common.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class BaseSendExtendDTO {
    // event name
    private String eventName;

    // event topic
    private String topic;

    // event tag
    private String tag;

    // event biz key
    private String keys;

    // message send timeout in milliseconds
    private Long sentTimeout;

    // delay time in milliseconds
    private Long delayTime;
}
