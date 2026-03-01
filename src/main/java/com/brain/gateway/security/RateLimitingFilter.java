package com.brain.gateway.security;

import com.brain.gateway.config.RouteConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RouteConfig routeConfig;
    private final RateLimiterService rateLimiterService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        Integer limit = null;
        String matchingPattern = null;

        // Find limit for the path
        for (Map.Entry<String, Integer> entry : routeConfig.getRateLimits().entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                matchingPattern = entry.getKey();
                limit = entry.getValue();
                break;
            }
        }

        if (limit != null) {
            String clientIp = getClientIp(request);
            String key = matchingPattern + ":" + clientIp;
            
            if (!rateLimiterService.isAllowed(key, limit)) {
                response.setStatus(429);
                response.getWriter().write("Too Many Requests. Limit: " + limit + " per minute.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
