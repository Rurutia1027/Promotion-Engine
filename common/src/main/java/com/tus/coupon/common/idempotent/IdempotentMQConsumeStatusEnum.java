package com.tus.coupon.common.idempotent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public enum IdempotentMQConsumeStatusEnum {
    /**
     * consuming
     */
    CONSUMING("0"),

    /**
     * consumed
     */
    CONSUMED("1");

    @Getter
    private final String code;

    /**
     * is consumed error
     */
    public static boolean isError(String consumeStatus) {
        return Objects.equals(CONSUMING.code, consumeStatus);
    }
}
