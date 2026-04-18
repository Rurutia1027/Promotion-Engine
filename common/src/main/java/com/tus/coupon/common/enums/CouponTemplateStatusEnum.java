package com.tus.coupon.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CouponTemplateStatusEnum {

    /**
     * 0: means coupon template still in valid status
     */
    ACTIVE(0),

    /**
     * 1: means coupon expiry, all its coupon instances cannot be used anymore
     */
    ENDED(1);

    @Getter
    private final int status;
}
