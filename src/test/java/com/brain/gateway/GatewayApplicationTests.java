package com.brain.gateway;

import com.brain.gateway.security.ProxyService;
import com.brain.gateway.security.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.CacheManager;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
})
class GatewayApplicationTests {

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private ProxyService proxyService;

    @Test
    void contextLoads() {
    }

}
