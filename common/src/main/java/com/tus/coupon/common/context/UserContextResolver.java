package com.tus.coupon.common.context;

import jakarta.servlet.http.HttpServletRequest;

public interface UserContextResolver {
    UserInfoDTO resolve(HttpServletRequest request);
}
