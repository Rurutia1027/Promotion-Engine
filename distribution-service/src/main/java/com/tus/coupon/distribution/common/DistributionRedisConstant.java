package com.tus.coupon.distribution.common;

public final class DistributionRedisConstant {

    /**
     * Coupon template delivery progress redis key
     */
    public static final String TEMPLATE_TASK_EXECUTE_PROGRESS_KEY = "coupon_distribution:template-task-execute-progress:%s";

    /**
     * Coupon batch save coupon receive user redis key
     */
    public static final String TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY = "coupon_distribution:template-task-execute-batch-user:%s";
}
