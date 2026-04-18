package com.tus.coupon.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// coupon delivery task status enumeration class
@RequiredArgsConstructor
public enum CouponTaskStatusEnum {
    // pending
    PENDING(0),
    // in progress
    IN_PROGRESS(1),

    // execution failure
    FAILED(2),

    // execution success
    SUCCESS(3),

    // cancel task
    CANCEL(4);

    @Getter
    private final int status;
}
