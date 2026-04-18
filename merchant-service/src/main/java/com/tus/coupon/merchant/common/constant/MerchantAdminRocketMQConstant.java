package com.tus.coupon.merchant.common.constant;

// TODO: when all constants extract to common constants, this class gonna be deleted
@Deprecated
public final class MerchantAdminRocketMQConstant {
    // coupon distribution schedule task Topic Key
    public static final String TEMPLATE_TASK_DELAY_TOPIC_KEY = "coupon_merchant-admin" +
            "-service_coupon-task-delay_topic${unique-name:}";

    // coupon distribution schedule task Consumer Group Key
    public static final String TEMPLATE_TASK_DELAY_STATUS_CG_KEY = "coupon_merchant-admin" +
            "-service_coupon-task-delay-status_cg${unique-name:}";

    // coupon template delivery schedule task Topic Key
    public static final String TEMPLATE_DELAY_TOPIC_KEY = "coupon_merchant-admin" +
            "-service_coupon-template-delay_topic${unique-name:}";

    // coupon template delivery schedule task status consumer key
    public static final String TEMPLATE_DELAY_STATUS_CG_KEY = "coupon_merchant-admin" +
            "-service_coupon-template-delay-status_cg${unique-name:}";
}
