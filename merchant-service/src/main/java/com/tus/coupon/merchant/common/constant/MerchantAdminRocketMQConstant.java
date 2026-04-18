package com.tus.coupon.merchant.common.constant;

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

    // coupon template delivery topick key
    // this topic internal records responsible for scanning coupon excel records, converting
    // into messages/events and delivery
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "coupon_distribution" +
            "-service_coupon-task-execute_topic${unique-name:}";
}
