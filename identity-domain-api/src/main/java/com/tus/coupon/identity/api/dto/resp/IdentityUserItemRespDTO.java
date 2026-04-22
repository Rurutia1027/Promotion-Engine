package com.tus.coupon.identity.api.dto.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdentityUserItemRespDTO {
    private Long id;
    private String userId;
    private String username;
    private String shopNumber;
    private String phone;
    private String mail;
}
