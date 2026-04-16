package com.tus.coupon.common.idempotent;

import com.tus.coupon.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@RequiredArgsConstructor
public final class NoMQDuplicateConsumeAspect {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local value = ARGV[1]
            local expire_time_ms = ARGV[2]
            return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
            """;

    @Around("@annotation(com.tus.coupon.common.idempotent.NoMQDuplicateConsume)")
    public Object noMQRepeatConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        NoMQDuplicateConsume noMQDuplicateConsume = getNoMQDuplicateConsumeAspect(joinPoint);
        String uniqueKey =
                noMQDuplicateConsume.keyPrefix() + SpELUtil.parseKey(noMQDuplicateConsume.key(),
                        ((MethodSignature) joinPoint.getSignature()).getMethod(),
                        joinPoint.getArgs());

        String absentAndGet = stringRedisTemplate.execute(
                RedisScript.of(LUA_SCRIPT, String.class),
                List.of(uniqueKey),
                IdempotentMQConsumeStatusEnum.CONSUMING.getCode(),
                String.valueOf(TimeUnit.SECONDS.toMillis(noMQDuplicateConsume.keyTimeout()))
        );

        if (Objects.nonNull(absentAndGet)) {
            boolean errorFlag = IdempotentMQConsumeStatusEnum.isError(absentAndGet);
            if (errorFlag) {
                throw new ServiceException(String.format("Message consumer idempotency " +
                        "issue, idempotency key %s", uniqueKey));
            }
            return null;
        }

        Object result;
        try {
            result = joinPoint.proceed();
            stringRedisTemplate.opsForValue().set(uniqueKey,
                    IdempotentMQConsumeStatusEnum.CONSUMED.getCode(),
                    noMQDuplicateConsume.keyTimeout(), TimeUnit.SECONDS);
        } catch (Throwable ex) {
            stringRedisTemplate.delete(uniqueKey);
            throw ex;
        }
        return result;
    }

    public static NoMQDuplicateConsume getNoMQDuplicateConsumeAspect(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod =
                joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(),
                        methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(NoMQDuplicateConsume.class);
    }
}
