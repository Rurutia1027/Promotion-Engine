package com.tus.coupon.common.config;

import com.tus.coupon.common.idempotent.NoDuplicateSubmitAspect;
import com.tus.coupon.common.idempotent.NoMQDuplicateConsumeAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

public class IdempotentConfiguration {
    @Bean
    public NoDuplicateSubmitAspect noDuplicateSubmitAspect(RedissonClient redissonClient) {
        return new NoDuplicateSubmitAspect(redissonClient);
    }

    @Bean
    public NoMQDuplicateConsumeAspect noMQDuplicateConsumeAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoMQDuplicateConsumeAspect(stringRedisTemplate);
    }
}
