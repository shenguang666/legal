package com.legal.security;

import com.legal.common.AppException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate stringRedisTemplate;

    public IdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void ensureUnique(AuthPrincipal principal, String scene, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw AppException.badRequest("requestId 不能为空");
        }
        String key = "api:idempotency:" + principal.userId() + ":" + scene + ":" + requestId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.FALSE.equals(success)) {
            throw AppException.badRequest("重复请求，请勿重试提交");
        }
    }
}
