package com.tus.coupon.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    // user id
    private String userId;

    // user name
    private String username;

    // shop number
    private Long shopNumber;

    // tenant id
    private String tenantId;

    // trace id
    private String traceId;
}
