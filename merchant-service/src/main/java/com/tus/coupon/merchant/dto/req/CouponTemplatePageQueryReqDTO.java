package com.tus.coupon.merchant.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Coupon Template Pagination Request DTO")
public class CouponTemplatePageQueryReqDTO extends Page {
    // coupon name
    @Schema(description = "coupon name")
    private String name;

    // Promotion object: 0: product specified, 1: storewided
    @Schema(description = "Eligible object, 0: product specified, 1: storewide")
    private Integer target;

    // coupon goods number
    @Schema(description = "coupon good number")
    private String goodsNumber;

    // Promotion type: 0: fixed-amount discount coupon ; 1: threshold-based discount coupon;
    // 2: percentage-based discount coupon
    @Schema(description = "promotion type")
    private Integer type;
}
