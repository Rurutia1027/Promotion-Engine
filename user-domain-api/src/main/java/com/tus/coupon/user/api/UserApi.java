package com.tus.coupon.user.api;

import com.tus.coupon.common.web.Result;
import com.tus.coupon.user.api.dto.resp.UserPageQueryRespDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserApi {
    @GetMapping("/api/users/merchant/query")
    Result<UserPageQueryRespDTO> queryMerchantUsers(
            @RequestParam String shopNumber,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit);


    // if userIds not given null or blank, we will load all user items and return
    @GetMapping("/api/users/consumer/batch")
    Result<UserPageQueryRespDTO> queryConsumerUsersBatch(
            @RequestParam String userIds
    );
}
