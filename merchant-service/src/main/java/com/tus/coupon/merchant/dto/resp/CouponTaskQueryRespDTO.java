package com.tus.coupon.merchant.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Coupon distribution task query response DTO")
public class CouponTaskQueryRespDTO {

    /**
     * batch id
     */
    @Schema(description = "batch id")
    private String batchId;

    /**
     * Coupon batch task name
     */
    @Schema(description = "Coupon batch task name")
    private String taskName;

    /**
     * Coupon release number
     */
    @Schema(description = "Coupon release number")
    private Integer sendNum;

    /**
     * Notification type (can be combinable): 0: in-site letter, 1: prompt message, 2:
     * email, 3: sms
     */
    @Schema(description = "Notification type (can be combinable): 0: in-site letter, 1: prompt message, " +
            "2:email, 3: sms")
    private String notifyType;

    /**
     * Coupon template id
     */
    @Schema(description = "Coupon template id")
    private String couponTemplateId;

    /**
     * Send type: 0: immediate, 1: scheduled
     */
    @Schema(description = "Send type: 0: immediate, 1: scheduled ")
    private Integer sendType;

    /**
     * Delivery time
     */
    @Schema(description = "Delivery time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    /**
     * Status: 0: pending, 1: in-progress, 2: execution failure, 3: execution success, 4:
     * cancel
     */
    @Schema(description = "Status: 0: pending, 1: in-progress, 2: execution failure, 3: " +
            "execution success, 4: cancel")
    private Integer status;

    /**
     * Complete time
     */
    @Schema(description = "complete time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completionTime;

    /**
     * Operator
     */
    @Schema(description = "Operator")
    private Long operatorId;
}
