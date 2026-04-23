package com.tus.coupon.distribution;

import com.tus.coupon.common.context.UserContextAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;


/**
 * Coupon distribution app
 */

@SpringBootApplication
@MapperScan("com.tus.coupon.common.dao.mapper")
@Import(UserContextAutoConfiguration.class)
@EnableFeignClients(basePackages = "com.tus.coupon.distribution.remote")
public class DistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributionApplication.class, args);
    }
}
