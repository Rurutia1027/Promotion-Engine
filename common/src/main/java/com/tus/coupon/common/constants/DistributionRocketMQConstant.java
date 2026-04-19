package com.tus.coupon.common.constants;

/**
 * Coupon delivery service RocketMQ Constant
 */
public final class DistributionRocketMQConstant {

    /**
     * coupon template delivery topic key
     * in charge of scanning Excel records, and convert each record into task to delivery
     */
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "coupon_distribution-service_coupon-task-execute_topic${unique-name:}";

    /**
     * coupon template delivery consumer group key
     */
    public static final String TEMPLATE_TASK_EXECUTE_CG_KEY = "coupon_distribution-service_coupon-task-execute_cg${unique-name:}";

    /**
     * coupon delivery topic key
     * in charge of delivery coupon to specific user
     */
    public static final String TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY = "coupon_distribution-service_coupon-execute-distribution_topic${unique-name:}";

    /**
     * coupon delivery topic consumer group key
     */
    public static final String TEMPLATE_EXECUTE_DISTRIBUTION_CG_KEY = "coupon_distribution-service_coupon-execute-distribution_cg${unique-name:}";

    /**
     * coupon delivery finish notify user consumer group key
     *
     * when upstream finish a batch of user coupon delivery with temporal number = batch
     * threshold or iterate the end of Excel file this stage finish,
     * an event will be generated and delivery to downstream MQ,
     * when downstream MQ subscriber/consumer receives this event it will fetch batch of
     * user info and iterate and notification message to them
     */
    public static final String TEMPLATE_EXECUTE_SEND_MESSAGE_CG_KEY = "coupon_distribution-service_coupon-execute-send-message_cg${unique-name:}";
}
