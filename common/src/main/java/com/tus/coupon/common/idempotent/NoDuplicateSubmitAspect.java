package com.tus.coupon.common.idempotent;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.tus.coupon.common.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@RequiredArgsConstructor
public final class NoDuplicateSubmitAspect {
    private final RedissonClient redissonClient;

    @Around("@annotation(com.tus.coupon.common.idempotent.NoDuplicateSubmit)")
    public Object noDuplicateSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        NoDuplicateSubmit noDuplicateSubmit = getNoDuplicateSubmitAnnotation(joinPoint);
        // fetch distributed flag
        String lockKey = String.format("no-duplicate-submit:path:%s:currentUserId:%s:md5:%s",
                getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint));
        RLock lock = redissonClient.getLock(lockKey);

        // try to fetch lock, if fetch lock failed it means duplicate submit, then throw
        // exception
        if (!lock.tryLock()) {
            throw new ClientException(noDuplicateSubmit.message());
        }

        // lock race fetch success, trigger aop execute and get execution return result
        Object ret;
        try {
            ret = joinPoint.proceed();
        } finally {
            lock.unlock();
        }
        return ret;
    }

    public static NoDuplicateSubmit getNoDuplicateSubmitAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod =
                joinPoint.getTarget().getClass().getDeclaredMethod(
                        methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(NoDuplicateSubmit.class);
    }

    private String getServletPath() {
        ServletRequestAttributes sra =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest().getServletPath();
    }

    private String getCurrentUserId() {
        return UUID.randomUUID().toString();
    }

    // get md5
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }
}
