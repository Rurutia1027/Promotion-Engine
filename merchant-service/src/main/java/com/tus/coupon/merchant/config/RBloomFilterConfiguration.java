package com.tus.coupon.merchant.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RBloomFilterConfiguration {
    @Bean
    public RBloomFilter<String> couponTemplateQueryBloomFilter(RedissonClient redissonClient,
                                                               @Value("${framework.cache.redis.prefix:}") String cachePrefix) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(cachePrefix + "couponTemplateQueryBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }
}
