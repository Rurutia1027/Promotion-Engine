package com.tus.coupon.common.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateDelayEvent {

    /**
     * shop number
     */
    private Long shopNumber;

    /**
     * coupon template id
     */
    private Long couponTemplateId;

    /**
     * specific delay timestamp
     */
    private Long delayTime;
}
