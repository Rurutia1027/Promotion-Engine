package com.tus.coupon.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tus.coupon.common.dao.entity.UserDO;
import com.tus.coupon.common.dao.mapper.UserMapper;
import com.tus.coupon.common.exception.ClientException;
import com.tus.coupon.user.api.dto.resp.UserItemRespDTO;
import com.tus.coupon.user.api.dto.resp.UserPageQueryRespDTO;
import com.tus.coupon.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final UserMapper userMapper;

    @Override
    public UserPageQueryRespDTO queryMerchantUsers(String shopNumber, Long cursor, Integer limit) {
        if (StrUtil.isBlank(shopNumber)) {
            throw new ClientException("shopNumber is required");
        }
        int safeLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;

        if (safeLimit > MAX_LIMIT) {
            throw new ClientException("Requested limit exceeds max allowed size: " + MAX_LIMIT);
        }

        long safeCursor = cursor == null ? 0L : cursor;

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .gt(UserDO::getId, safeCursor)
                .eq(UserDO::getDelFlag, 0)
                .eq(UserDO::getUserType, "MERCHANT")
                .eq(UserDO::getShopNumber, shopNumber)
                .orderByAsc(UserDO::getId)
                .last("LIMIT " + safeLimit);

        List<UserDO> userDOList = userMapper.selectList(queryWrapper);
        return buildPageResponse(userDOList, safeCursor, safeLimit);
    }

    @Override
    public UserPageQueryRespDTO queryConsumerUsersBatch(String userIds) {
        if (StrUtil.isBlank(userIds)) {
            throw new ClientException("userIds is required");
        }
        List<Long> parsedUserIds = StrUtil.splitTrim(userIds, ",").stream()
                .filter(StrUtil::isNotBlank)
                .map(each -> {
                    if (!StrUtil.isNumeric(each)) {
                        throw new ClientException("userIds must be a comma-separated numeric list");
                    }
                    return Long.parseLong(each);
                })
                .toList();

        if (parsedUserIds.isEmpty()) {
            return UserPageQueryRespDTO.builder()
                    .users(Collections.emptyList())
                    .hasMore(Boolean.FALSE)
                    .nextCursor(0L)
                    .build();
        }

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getDelFlag, 0)
                .eq(UserDO::getUserType, "CONSUMER")
                .in(UserDO::getId, parsedUserIds)
                .orderByAsc(UserDO::getId);

        List<UserDO> userDOList = userMapper.selectList(queryWrapper);
        return buildPageResponse(userDOList, 0L, parsedUserIds.size());
    }

    private UserPageQueryRespDTO buildPageResponse(List<UserDO> userDOList, Long cursor, int limit) {
        if (userDOList == null || userDOList.isEmpty()) {
            return UserPageQueryRespDTO.builder()
                    .users(Collections.emptyList())
                    .hasMore(Boolean.FALSE)
                    .nextCursor(cursor)
                    .build();
        }

        List<UserItemRespDTO> users = userDOList.stream()
                .map(each -> UserItemRespDTO.builder()
                        .id(each.getId())
                        .userId(String.valueOf(each.getId()))
                        .username(each.getUsername())
                        .userType(each.getUserType())
                        .shopNumber(each.getShopNumber())
                        .phone(each.getPhone())
                        .mail(each.getMail())
                        .build())
                .toList();

        Long nextCursor = userDOList.get(userDOList.size() - 1).getId();
        boolean hasMore = userDOList.size() >= limit;
        return UserPageQueryRespDTO.builder()
                .users(users)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
