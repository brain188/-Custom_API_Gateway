package com.brain.gateway.integration;

import com.brain.gateway.security.JwtUtil;
import com.brain.gateway.security.ProxyService;
import com.brain.gateway.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
})
class GatewayIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private ProxyService proxyService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CacheManager cacheManager;

    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        validToken = jwtUtil.generateToken("user", List.of("ROLE_USER"));
    }

    @Test
    void testAuthenticatedRouting() throws Exception {
        // Mock ProxyService response
        ProxyService.CachedResponse mockResponse = new ProxyService.CachedResponse(
                200, Map.of("Content-Type", "application/json"), "{\"data\":\"success\"}".getBytes()
        );
        
        when(proxyService.executeRequest(anyString(), anyString(), anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockResponse);
        
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);

        mockMvc.perform(get("/api/users/123")
                .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("{\"data\":\"success\"}"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Gateway-Name", "Brain-API-Gateway"));
    }

    @Test
    void testRateLimiting() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(get("/api/users/123")
                .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.CoreMatchers.containsString("Too Many Requests")));
    }
}
