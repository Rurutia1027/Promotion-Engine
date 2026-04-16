package com.tus.coupon.merchant.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class CouponTaskCreateReqDTO {
    // coupon batch task name
    @Schema(description = "coupon batch task name",
            example = "Send Million-Coupon Push Notification Task",
            required = true)
    private String taskName;

    // file address
    @Schema(description = "coupon batch task name",
            // step-1: invoke ExcelGenerateTests internal unit case generate Excel demo files
            // step-2: cpy ut-case generated Excel files to target path, let, task trigger
            // parse and convert into tasks
            example = "/xxx/xx.xlsx",
            required = true
    )
    private String fileAddress;

    // notification types (can be combinable): 0: site-internal, 1: prompt notification, 2:
    // email, 3: sms
    @Schema(description = "notification type",
            example = "0,3",
            required = true)
    private String notifyType;

    // coupon template id
    @Schema(description = "coupon template id",
            example = "712839405345",
            // todo refine this into fomrat like COUPOIN_TEMPLATE_ID- some string as prefix
            required = true)
    private String couponTemplateId;

    // send type: 0: send immediately, 1: send scheduled
    @Schema(description = "send type",
            example = "0",
            required = true)
    private Integer sendType;

    // send timestamp
    @Schema(description = "send time", example = "2026-08-20 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;
}
