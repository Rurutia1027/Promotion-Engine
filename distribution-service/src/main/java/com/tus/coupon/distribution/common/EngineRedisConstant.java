package com.tus.coupon.distribution.common;

public final class EngineRedisConstant {
    // coupon template cache key
    public static final String COUPON_TEMPLATE_KEY = "coupon_engine:template:%s";

    // user coupon template key
    public static final String USER_COUPON_TEMPLATE_LIST_KEY = "coupon_engine:user-template" +
            "-list:";

    // user coupon accept time limit key
    public static final String USER_COUPON_TEMPLATE_LIMIT_KEY = "coupon_engine:user-template" +
            "-limit:";
}
