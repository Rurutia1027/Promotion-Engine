package com.tus.coupon.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CouponSourceEnum {

    /**
     * Shop coupon
     */
    SHOP(0),

    /**
     * Platform coupon
     */
    PLATFORM(1);

    @Getter
    private final int type;
}
