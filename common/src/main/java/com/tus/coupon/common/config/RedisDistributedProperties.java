package com.tus.coupon.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX)
public class RedisDistributedProperties {
    public static final String PREFIX = "common.cache.redis";

    // prefix
    private String prefix;

    // key prefix charset
    private String prefixCharset = "UTF-8";
}
