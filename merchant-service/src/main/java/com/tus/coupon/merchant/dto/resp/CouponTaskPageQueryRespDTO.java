package com.tus.coupon.merchant.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "coupon schedule job pagination response DTO")
public class CouponTaskPageQueryRespDTO {
    // batch id
    @Schema(description = "batch id")
    private String batchId;

    // coupon batch task name
    @Schema(description = "coupon batch task name")
    private String taskName;

    // coupon release number
    @Schema(description = "coupon distribution number")
    private Integer sendNum;

    /**
     * Notification types (combinable): 0: in-site letter, 1: prompt message, 2: email, 3: sms
     */
    @Schema(description = "Notification types (combinable): 0: in-site letter, " +
            "1: prompt message, 2: email, 3: sms")
    private String notifyType;

    /**
     * Coupon template id
     */
    @Schema(description = "Coupon template id")
    private String couponTemplateId;

    /**
     * Send type 0: send immediately, 1: scheduled send
     */
    @Schema(description = "发送类型，0：立即发送 1：定时发送")
    private Integer sendType;

    /**
     * Delivery time
     */
    @Schema(description = "Delivery time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    /**
     * Status 0: pending, 1: in progress, 2: execution failure, 3: execution success, 4:
     * cancel
     */
    @Schema(description = "Status, 0: pending, 1: in progress, 2: execution failure, 3: " +
            "execution success, 4: Cancel")
    private Integer status;

    /**
     * Completion time
     */
    @Schema(description = "Completion Time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completionTime;

    /**
     * operator
     */
    @Schema(description = "operator")
    private Long operatorId;
}
