package com.tus.coupon.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tus.coupon.common.idempotent.NoDuplicateSubmit;
import com.tus.coupon.common.web.Result;
import com.tus.coupon.common.web.Results;
import com.tus.coupon.merchant.dto.req.CouponTaskCreateReqDTO;
import com.tus.coupon.merchant.dto.req.CouponTaskPageQueryReqDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskPageQueryRespDTO;
import com.tus.coupon.merchant.dto.resp.CouponTaskQueryRespDTO;
import com.tus.coupon.merchant.service.CouponTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Coupon Task Delivery Controller")
public class CouponTaskController {
    private final CouponTaskService couponTaskService;

    @Operation(summary = "merchant create coupon deliver task")
    @NoDuplicateSubmit(message = "please do not commit too frequently")
    @PostMapping("/api/merchant-admin/coupon-task/create")
    public Result<Void> createCouponTask(@RequestBody CouponTaskCreateReqDTO requestParam) {
        couponTaskService.createCouponDeliverTask(requestParam);
        return Results.success();
    }

    @Operation(summary = "Pagination query coupon delivery tasks")
    @GetMapping("/api/merchant-admin/coupon-task/page")
    public Result<IPage<CouponTaskPageQueryRespDTO>> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        return Results.success(couponTaskService.pageQueryCouponTask(requestParam));
    }

    @Operation(summary = "Query coupon delivery task detail")
    @GetMapping("/api/merchant-admin/coupon-task/find")
    public Result<CouponTaskQueryRespDTO> findCouponTaskById(String taskId) {
        return Results.success(couponTaskService.findCouponTaskById(taskId));
    }
}
