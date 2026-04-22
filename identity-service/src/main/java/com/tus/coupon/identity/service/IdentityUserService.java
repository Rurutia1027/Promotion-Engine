package com.tus.coupon.identity.service;

import com.tus.coupon.identity.api.dto.req.IdentityUserQueryReqDTO;
import com.tus.coupon.identity.api.dto.resp.IdentityUserQueryRespDTO;

public interface IdentityUserService {
    IdentityUserQueryRespDTO queryUsers(IdentityUserQueryReqDTO requestParam);
}
