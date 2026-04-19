package com.tus.coupon.distribution.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateDistributionEvent {

    /**
     * Coupon delivery task id
     */
    private Long couponTaskId;

    /**
     * Coupon delivery task batch id
     */
    private Long couponTaskBatchId;

    /**
     * notify type (can be combined):0: in site message, 1: prompt message, 2: email, 3: sms
     */
    private String notifyType;

    /**
     * shopNumber
     */
    private Long shopNumber;

    /**
     * coupon template id
     */
    private Long couponTemplateId;

    /**
     * validity start time
     */
    private Date validEndTime;

    /**
     * coupon consumption rule
     */
    private String couponTemplateConsumeRule;

    /**
     * user id which user receive this coupon
     */
    private String userId;

    /**
     * phone number of the user who receive the coupon
     */
    private String phone;

    /**
     * mail
     */
    private String mail;

    /**
     * batch user set size , user set hold on redis = how many users in total receive
     * pre-allocated distributed coupons, only when user set size attach 5000 will it
     * trigger the downstream message delivery
     */
    private Integer batchUserSetSize;

    /**
     * coupon delivery complete flag
     */
    private Boolean distributionEndFlag;
}
