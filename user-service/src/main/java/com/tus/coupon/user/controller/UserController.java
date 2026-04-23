package com.tus.coupon.user.controller;

import com.tus.coupon.common.web.Result;
import com.tus.coupon.common.web.Results;
import com.tus.coupon.user.api.UserApi;
import com.tus.coupon.user.api.dto.resp.UserPageQueryRespDTO;
import com.tus.coupon.user.service.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Controller")
public class UserController implements UserApi {
    private final UserQueryService userQueryService;

    @Override
    @Operation(summary = "Query merchant users by shop number")
    public Result<UserPageQueryRespDTO> queryMerchantUsers(
            @RequestParam String shopNumber,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return Results.success(userQueryService.queryMerchantUsers(shopNumber, cursor, limit));
    }

    @Override
    @Operation(summary = "Batch query consumer users by user ids")
    public Result<UserPageQueryRespDTO> queryConsumerUsersBatch(@RequestParam String userIds) {
        return Results.success(userQueryService.queryConsumerUsersBatch(userIds));
    }
}
