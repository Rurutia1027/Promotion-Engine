package com.tus.coupon.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CouponStatusEnum {

    /**
     * Coupon is effective
     */
    EFFECTIVE(0),

    /**
     * Coupon expiry
     */
    ENDED(1);

    @Getter
    private final int type;
}
