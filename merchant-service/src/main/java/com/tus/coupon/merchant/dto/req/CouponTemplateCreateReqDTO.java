package com.tus.coupon.merchant.dto.req;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Coupon Template Creation Request DTO")
public class CouponTemplateCreateReqDTO {
    // coupon name
    @Schema(description = "coupon name",
            example = "Extra-Special Offer: $3 Off Orders of $10 or More",
            required = true)
    private String name;

    // source of the coupon 0: store/shop specified, 1: platform
    @Schema(description = "source of the coupon",
            example = "0",
            required = true)
    private Integer source;

    // Promotion object: 0: product/good specified, 1: store/shop wide
    @Schema(description = "promotion object",
            example = "1", required = true)
    private Integer target;

    // Promotion product/goods number
    @Schema(description = "Promotion product/good number")
    private String goods;

    // Promotion type: 0: Fixed-amount discount coupon, 1: threshold discount coupon
    // 2: percentage based discount coupon
    @Schema(description = "promotion type",
            example = "0",
            required = true)
    private Integer type;

    // validity start date
    @Schema(description = "validity start date",
            example = "2025-07-08 12:00:00",
            required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    /**
     * Validity end date
     */
    @Schema(description = "validity end date",
            example = "2026-07-08 12:00:00",
            required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    /**
     * Coupon stock number
     */
    @Schema(description = "Stock",
            example = "200",
            required = true)
    private Integer stock;

    /**
     * Coupon claim rule
     */
    @Schema(description = "Claim rule",
            example = "{\"limitPerPerson\":1,\"usageInstructions\":\"3\"}",
            required = true)
    private String receiveRule;

    /**
     * Coupon consuming rule
     */
    @Schema(description = "consuming rule",
            example = "{\"termsOfUse\":10,\"maximumDiscountAmount\":3,\"explanationOfUnmetConditions\":\"3\",\"validityPeriod\":\"48\"}",
            required = true)
    private String consumeRule;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
