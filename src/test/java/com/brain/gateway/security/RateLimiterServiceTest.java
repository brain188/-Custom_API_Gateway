package com.brain.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testAllowedUnderLimit() {
        String key = "test-key";
        int limit = 5;
        
        when(valueOperations.increment(eq("rate_limit:" + key))).thenReturn(1L);
        
        boolean allowed = rateLimiterService.isAllowed(key, limit);
        
        assertTrue(allowed);
        verify(redisTemplate).expire(eq("rate_limit:" + key), any(Duration.class));
    }

    @Test
    void testBlockedOverLimit() {
        String key = "test-key";
        int limit = 5;
        
        when(valueOperations.increment(eq("rate_limit:" + key))).thenReturn(6L);
        
        boolean allowed = rateLimiterService.isAllowed(key, limit);
        
        assertFalse(allowed);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void testExistingWindowDoesNotResetTTL() {
        String key = "test-key";
        int limit = 5;
        
        when(valueOperations.increment(eq("rate_limit:" + key))).thenReturn(2L);
        
        boolean allowed = rateLimiterService.isAllowed(key, limit);
        
        assertTrue(allowed);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }
}
