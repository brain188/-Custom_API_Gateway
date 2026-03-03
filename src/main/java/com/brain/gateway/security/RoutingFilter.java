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
import java.util.Enumeration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoutingFilter extends OncePerRequestFilter {

    private final RouteConfig routeConfig;
    private final ProxyService proxyService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String targetBaseUrl = null;
        boolean isCacheablePattern = false;

        for (Map.Entry<String, String> entry : routeConfig.getRoutes().entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                targetBaseUrl = entry.getValue();
                isCacheablePattern = routeConfig.getCacheable().getOrDefault(entry.getKey(), false);
                break;
            }
        }

        if (targetBaseUrl == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check for cache bypass
        boolean bypassCache = "no-cache".equalsIgnoreCase(request.getHeader("X-No-Cache"));
        boolean shouldCache = isCacheablePattern && !bypassCache;

        try {
            Map<String, String> headers = new java.util.HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }

            byte[] body = null;
            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                body = request.getInputStream().readAllBytes();
            }

            ProxyService.CachedResponse proxyResponse = proxyService.executeRequest(
                    request.getMethod(),
                    targetBaseUrl,
                    path,
                    body,
                    headers,
                    shouldCache
            );

            response.setStatus(proxyResponse.status());
            proxyResponse.headers().forEach(response::addHeader);
            if (proxyResponse.body() != null) {
                response.getOutputStream().write(proxyResponse.body());
            }
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("Routing error: " + e.getMessage());
        }
    }
}
