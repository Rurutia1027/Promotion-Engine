package com.tus.coupon.identity.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tus.coupon.common.dao.entity.UserDO;
import com.tus.coupon.common.dao.mapper.UserMapper;
import com.tus.coupon.common.exception.ClientException;
import com.tus.coupon.identity.api.dto.req.IdentityUserQueryReqDTO;
import com.tus.coupon.identity.api.dto.resp.IdentityUserItemRespDTO;
import com.tus.coupon.identity.api.dto.resp.IdentityUserQueryRespDTO;
import com.tus.coupon.identity.service.IdentityUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityUserServiceImpl implements IdentityUserService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final UserMapper userMapper;

    @Override
    public IdentityUserQueryRespDTO queryUsers(IdentityUserQueryReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Identity user query request should not be null");
        }

        Integer limit = requestParam.getLimit();
        if (limit == null || limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        if (limit > MAX_LIMIT) {
            throw new ClientException("Requested limit exceeds max allowed size: " + MAX_LIMIT);
        }

        long cursor = requestParam.getCursor() == null ? 0L : requestParam.getCursor();
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .gt(UserDO::getId, cursor)
                .eq(UserDO::getDelFlag, 0)
                .eq(StrUtil.isNotBlank(requestParam.getShopNumber()), UserDO::getShopNumber, requestParam.getShopNumber())
                .orderByAsc(UserDO::getId)
                .last("LIMIT " + limit);

        List<UserDO> userDOList = userMapper.selectList(queryWrapper);
        if (userDOList == null || userDOList.isEmpty()) {
            return IdentityUserQueryRespDTO.builder()
                    .users(Collections.emptyList())
                    .hasMore(Boolean.FALSE)
                    .nextCursor(cursor)
                    .build();
        }

        List<IdentityUserItemRespDTO> users = userDOList.stream()
                .map(each -> IdentityUserItemRespDTO.builder()
                        .id(each.getId())
                        .userId(String.valueOf(each.getId()))
                        .username(each.getUsername())
                        .shopNumber(each.getShopNumber())
                        .phone(each.getPhone())
                        .mail(each.getMail())
                        .build())
                .toList();

        Long nextCursor = userDOList.get(userDOList.size() - 1).getId();
        boolean hasMore = userDOList.size() >= limit;
        return IdentityUserQueryRespDTO.builder()
                .users(users)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
