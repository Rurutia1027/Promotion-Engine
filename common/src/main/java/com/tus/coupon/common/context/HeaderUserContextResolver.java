package com.tus.coupon.common.context;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public class HeaderUserContextResolver implements UserContextResolver {
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_SHOP_NUMBER = "X-Shop-Number";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";


    @Override
    public UserInfoDTO resolve(HttpServletRequest request) {
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);
        String shopNumberStr = request.getHeader(HEADER_SHOP_NUMBER);
        String tenantId = request.getHeader(HEADER_TENANT_ID);
        String traceId = request.getHeader(HEADER_TRACE_ID);

        Long shopNumber = null;
        if (StrUtil.isNotBlank(shopNumberStr) && StrUtil.isNumeric(shopNumberStr)) {
            shopNumber = Long.parseLong(shopNumberStr);
        }

        if (StrUtil.isBlank(userId)) {
            userId = "anonymous";
        }
        if (StrUtil.isBlank(username)) {
            username = "anonymous";
        }
        if (StrUtil.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        return UserInfoDTO.builder()
                .userId(userId)
                .username(username)
                .shopNumber(shopNumber)
                .tenantId(tenantId)
                .traceId(traceId)
                .build();
    }
}
