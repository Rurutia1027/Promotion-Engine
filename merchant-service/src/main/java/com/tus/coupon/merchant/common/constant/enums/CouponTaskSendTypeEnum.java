package com.tus.coupon.merchant.common.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CouponTaskSendTypeEnum {

    /**
     * Send immediately
     */
    IMMEDIATE(0),

    /**
     * Send delay
     */
    SCHEDULED(1);

    @Getter
    private final int type;
}
