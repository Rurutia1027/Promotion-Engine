package com.tus.coupon.common.context;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class UserContextAutoConfiguration {

    @Bean
    public UserContextResolver userContextResolver() {
        return new HeaderUserContextResolver();
    }

    @Bean
    public UserContextInterceptor userContextInterceptor(UserContextResolver userContextResolver) {
        return new UserContextInterceptor(userContextResolver);
    }

    @Bean
    public WebMvcConfigurer userContextWebMvcConfigurer(UserContextInterceptor userContextInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(userContextInterceptor).addPathPatterns("/**");
            }
        };
    }
}
