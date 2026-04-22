package com.tus.coupon.identity.api.dto.req;

import lombok.Data;

@Data
public class IdentityUserQueryReqDTO {
    // optional: query users by coupon task id
    private Long taskId;

    // optional: query users by merchant shop number
    private String shopNumber;

    // cursor based pagination, use user table id
    private Long cursor;

    // page size
    private Integer limit;
}
