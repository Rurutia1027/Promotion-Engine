package com.tus.coupon.identity.api.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IdentityUserQueryRespDTO {
    private List<IdentityUserItemRespDTO> users;
    private Long nextCursor;
    private Boolean hasMore;
}
