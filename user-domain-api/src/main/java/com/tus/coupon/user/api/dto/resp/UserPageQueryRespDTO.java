package com.tus.coupon.user.api.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPageQueryRespDTO {
    private List<UserItemRespDTO> users;
    private Long nextCursor;
    private Boolean hasMore;
}
