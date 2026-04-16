package com.tus.coupon.common.config;

import com.tus.coupon.common.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;

public class WebAutoConfiguration {
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
