package com.tus.coupon.merchant.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Pagination Query Parameters for Coupon Push Tasks")
public class CouponTaskPageQueryReqDTO extends Page {
    // batch id
    @Schema(description = "batch id")
    private String batchId;

    // coupon batch task name
    @Schema(description = "coupon batch task name")
    private String taskName;

    // coupon template id
    @Schema(description = "coupon template id")
    private String couponTemplateId;

    // status: 0: pending, 1: in progress, 2: failure, 3: success, 4: cancel
    @Schema(description = "status 0: pending, 1: in-progress, 2: failure, 3: success, 4: " +
            "cancel")
    private Integer status;
}
