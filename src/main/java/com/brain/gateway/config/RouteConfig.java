package com.brain.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class RouteConfig {
    /**
     * Map of path patterns to target service URLs.
     * Example: "/api/users/**" -> "http://localhost:8081"
     */
    private Map<String, String> routes = new HashMap<>();

    /**
     * Map of path patterns to requests per minute.
     * Example: "/api/users/**" -> 10
     */
    private Map<String, Integer> rateLimits = new HashMap<>();
}
