package com.tus.coupon.common.constants;

public final class MerchantRocketMQConstant {
    /**
     * Coupon template delay deliver topic key
     */
    public static final String TEMPLATE_DELAY_TOPIC_KEY = "coupon_merchant-admin-service_coupon-template-delay_topic${unique-name:}";

    /**
     * Coupon template delay deliver consumer group key
     */
    public static final String TEMPLATE_DELAY_STATUS_CG_KEY = "coupon_merchant-admin-service_coupon-template-delay-status_cg${unique-name:}";

}
