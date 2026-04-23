package com.tus.coupon.user.service;

import com.tus.coupon.user.api.dto.resp.UserPageQueryRespDTO;

public interface UserQueryService {
    UserPageQueryRespDTO queryMerchantUsers(String shopNumber, Long cursor, Integer limit);

    UserPageQueryRespDTO queryConsumerUsersBatch(String userIds);
}
