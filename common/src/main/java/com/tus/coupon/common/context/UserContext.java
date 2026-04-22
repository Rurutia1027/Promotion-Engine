package com.tus.coupon.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.tus.coupon.common.exception.ClientException;

import java.util.Optional;

public final class UserContext {
    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL =
            new TransmittableThreadLocal<>();

    // set user context
    public static void setUser(UserInfoDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static UserInfoDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static UserInfoDTO requireUser() {
        return Optional.ofNullable(USER_THREAD_LOCAL.get())
                .orElseThrow(() -> new ClientException("User context is required but missing"));
    }

    // fetch user id from user context
    public static String getUserId() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    // fetch username from user context
    public static String getUsername() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    // fetch shop number from user context
    public static Long getShopNumber() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getShopNumber).orElse(null);
    }

    public static String getTenantId() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getTenantId).orElse(null);
    }

    public static String getTraceId() {
        UserInfoDTO userInfoDTO = USER_THREAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getTraceId).orElse(null);
    }


    // clean user context
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
