package com.tus.coupon.identity.controller;

import com.tus.coupon.common.web.Result;
import com.tus.coupon.common.web.Results;
import com.tus.coupon.identity.api.dto.IdentityUserApi;
import com.tus.coupon.identity.api.dto.req.IdentityUserQueryReqDTO;
import com.tus.coupon.identity.api.dto.resp.IdentityUserQueryRespDTO;
import com.tus.coupon.identity.service.IdentityUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Identity User Controller")
public class IdentityUserController implements IdentityUserApi {
    private final IdentityUserService identityUserService;

    @Override
    @Operation(summary = "Query users for coupon distribution")
    public Result<IdentityUserQueryRespDTO> queryUsers(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String shopNumber,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        IdentityUserQueryReqDTO request = IdentityUserApi.buildReq(taskId, shopNumber, cursor, limit);
        return Results.success(identityUserService.queryUsers(request));
    }
}