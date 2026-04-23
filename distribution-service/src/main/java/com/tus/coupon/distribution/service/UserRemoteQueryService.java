package com.tus.coupon.distribution.service;

import com.tus.coupon.common.exception.ClientException;
import com.tus.coupon.common.web.Result;
import com.tus.coupon.distribution.remote.UserRemoteClient;
import com.tus.coupon.user.api.dto.resp.UserItemRespDTO;
import com.tus.coupon.user.api.dto.resp.UserPageQueryRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRemoteQueryService {
    private final UserRemoteClient userRemoteClient;

    public List<UserItemRespDTO> queryMerchantUsers(String shopNumber, Long cursor,
                                                    Integer limit) {
        Result<UserPageQueryRespDTO> result = userRemoteClient.queryMerchantUsers(shopNumber, cursor, limit);
        if (result == null) {
            throw new ClientException("Query merchant users failed: null response");
        }
        if (result.isFail()) {
            throw new ClientException("Query merchant users failed: " + result.getMessage());
        }

        UserPageQueryRespDTO data = result.getData();
        if (data == null || data.getUsers() == null) {
            return Collections.emptyList();
        }
        return data.getUsers();
    }

}
