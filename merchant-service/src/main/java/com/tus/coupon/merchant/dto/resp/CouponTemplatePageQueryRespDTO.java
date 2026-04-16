package com.tus.coupon.merchant.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Coupon Template Pagination Query Response DTO")
public class CouponTemplatePageQueryRespDTO {

    /**
     * Coupon name
     */
    @Schema(description = "Coupon name")
    private String name;

    /**
     * Coupon source: 0: store specified, 1: platform coupon
     */
    @Schema(description = "Coupon source: 0: store specified, 1: platform coupon")
    private Integer source;

    /**
     * Promotion object: 0: product specified, 1: store wide
     */
    @Schema(description = "Promotion object: 0: product specified, 1: store wide")
    private Integer target;

    /**
     * Promotion number for goods/products
     */
    @Schema(description = "Promotion product/goods number")
    private String goods;

    /**
     * Type of coupon: 0: instant coupon, 1: threshold based coupon, 3: discount based
     * coupon
     */
    @Schema(description = "0: instant coupon, 1: threshold based coupon, 3: discount based " +
            "coupon")
    private Integer type;

    /**
     * Validity star time
     */
    @Schema(description = "Validity start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    /**
     * Validity end time
     */
    @Schema(description = "validity end time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    /**
     * Stock of coupon
     */
    @Schema(description = "Stock of coupon")
    private Integer stock;

    /**
     * Claim rule
     */
    @Schema(description = "claim rule")
    private String receiveRule;

    /**
     * consume rule
     */
    @Schema(description = "consume rule")
    private String consumeRule;
}