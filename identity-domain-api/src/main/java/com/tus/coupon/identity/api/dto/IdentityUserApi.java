package com.tus.coupon.identity.api.dto;

import com.tus.coupon.common.web.Result;
import com.tus.coupon.identity.api.dto.req.IdentityUserQueryReqDTO;
import com.tus.coupon.identity.api.dto.resp.IdentityUserQueryRespDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface IdentityUserApi {

    @GetMapping("/api/identity/users/query")
    Result<IdentityUserQueryRespDTO> queryUsers(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String shopNumber,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    );

    static IdentityUserQueryReqDTO buildReq(
            Long taskId,
            String shopNumber,
            Long cursor,
            Integer limit
    ) {
        IdentityUserQueryReqDTO request = new IdentityUserQueryReqDTO();
        request.setTaskId(taskId);
        request.setShopNumber(shopNumber);
        request.setCursor(cursor);
        request.setLimit(limit);
        return request;
    }
}
