package com.tus.coupon.common.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public final class MessageWrapper<T> implements Serializable {
    private static final long serialVersionUID = 1238490523456L;

    // message delivery keys
    @NonNull
    private String keys;

    // message payload
    @NonNull
    private T message;

    // message send timestamp
    private Long timestamp = System.currentTimeMillis();
}
