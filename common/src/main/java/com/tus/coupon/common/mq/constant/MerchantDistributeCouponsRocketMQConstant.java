package com.tus.coupon.common.mq.constant;

public final class MerchantDistributeCouponsRocketMQConstant {

    // coupon template delivery topic key
    // this topic internal records responsible for scanning coupon excel records, converting
    // into messages/events and delivery
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "coupon_distribution" +
            "-service_coupon-task-execute_topic${unique-name:}";

    // coupon template delivery execute topic's consumer group
    public static final String TEMPLATE_TASK_EXECUTE_CG_KEY = "coupon_distribution" +
            "-service_coupon-task-execute_cg${unique-name:}";
}
