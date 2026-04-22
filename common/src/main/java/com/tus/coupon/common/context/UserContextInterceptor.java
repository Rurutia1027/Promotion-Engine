package com.tus.coupon.common.context;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {
    private final UserContextResolver userContextResolver;

    public UserContextInterceptor(UserContextResolver userContextResolver) {
        this.userContextResolver = userContextResolver;
    }

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request,
                             @Nullable HttpServletResponse response,
                             @Nullable Object handler) throws Exception {
        if (request != null) {
            UserContext.setUser(userContextResolver.resolve(request));
        }
        return true;
    }

    @Override
    public void afterCompletion(@Nullable HttpServletRequest request,
                                @Nullable HttpServletResponse response,
                                @Nullable Object handler,
                                Exception ex) throws Exception {
        UserContext.removeUser();
    }
}
