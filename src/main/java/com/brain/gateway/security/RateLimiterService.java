package com.brain.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isAllowed(String key, int limit) {
        // Use a 1-minute fixed window
        String redisKey = "rate_limit:" + key;
        
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count != null && count == 1) {
            // New window, set expiration
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }
        
        return count != null && count <= limit;
    }
}
