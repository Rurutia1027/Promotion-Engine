package com.tus.coupon.merchant.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "increase release coupon template number request dto")
public class CouponTemplateNumberReqDTO {
    // coupon template id
    @Schema(description = "coupon template id",
            example = "2839405434534",// i prefer add some string as coupon template prefix
            // for better validation and pattern matching
            required = true
    )
    private String couponTemplateId;

    // request increase distribution number value
    @Schema(description = "increased distribution number value",
            example = "100",
            required = true)
    private Integer number;
}
