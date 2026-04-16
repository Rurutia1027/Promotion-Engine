package com.tus.coupon.merchant.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Coupon Template Query Response DTO")
public class CouponTemplateQueryRespDTO {


    /**
     * Coupon Id
     */
    @Schema(description = "Coupon id")
    private String id;

    /**
     * Coupon name
     */
    @Schema(description = "Coupon name")
    private String name;

    /**
     * Shop number
     */
    @Schema(description = "Shop number")
    private String shopNumber;

    /**
     * Coupon Source 0: Store specified coupon, 1: platform specified coupon
     */
    @Schema(description = "Coupon Source 0: Store coupon, 1: Platform coupon")
    private Integer source;

    /**
     * Promotion object: 0: product specified, 1: store specified
     */
    @Schema(description = "Promotion object: 0: product specified, 1: store specified")
    private Integer target;

    /**
     * Coupon product number
     */
    @Schema(description = "Coupon product number")
    private String goods;

    /**
     * Coupon type: 0 instantly coupon, 1: threshold-based coupon, 2: discount-based coupon
     */
    @Schema(description = "Coupon type: 0 instantly coupon, 1: threshold-based coupon, 2: " +
            "discount-based coupon")
    private Integer type;

    /**
     * Validity date
     */
    @Schema(description = "validity start date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    /**
     * validity end date
     */
    @Schema(description = "validity end date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    /**
     * Coupon stock
     */
    @Schema(description = "Coupon stock")
    private Integer stock;

    /**
     * Claim rule
     */
    @Schema(description = "Claim rule")
    private String receiveRule;

    /**
     * Coupon consuming rule
     */
    @Schema(description = "consuming rule")
    private String consumeRule;

    /**
     * Status of coupon: 0: valid, 1: invalid
     */
    @Schema(description = "Status of coupon: 0: valid, 1: invalid")
    private Integer status;
}
