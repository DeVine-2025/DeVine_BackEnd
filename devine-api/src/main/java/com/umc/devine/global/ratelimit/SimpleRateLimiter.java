package com.umc.devine.global.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis INCR 기반의 단순 고정 윈도우 레이트 리미터.
 * 브루트포스성 요청(예: 쿠폰 코드 추측)을 IP 단위로 제한하는 용도.
 */
@Component
@RequiredArgsConstructor
public class SimpleRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count <= limit;
    }
}
