package com.brain.gateway.security;

import com.brain.gateway.config.RouteConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoutingFilter extends OncePerRequestFilter {

    private final RouteConfig routeConfig;
    private final RestClient restClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip routing for auth endpoints and static resources
        return path.startsWith("/auth/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String targetBaseUrl = null;
        String matchingPattern = null;

        for (Map.Entry<String, String> entry : routeConfig.getRoutes().entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                matchingPattern = entry.getKey();
                targetBaseUrl = entry.getValue();
                break;
            }
        }

        if (targetBaseUrl == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Forward request
        proxyRequest(request, response, targetBaseUrl);
    }

    private void proxyRequest(HttpServletRequest request, HttpServletResponse response, String targetBaseUrl) throws IOException {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        RestClient.RequestBodySpec requestSpec = restClient.method(HttpMethod.valueOf(method))
                .uri(targetBaseUrl + path);

        // Copy headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host")) { // Avoid host header conflict
                requestSpec.header(headerName, request.getHeader(headerName));
            }
        }

        // Forward body if present
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            requestSpec.body(request.getInputStream().readAllBytes());
        }

        try {
            ResponseEntity<byte[]> targetResponse = requestSpec.retrieve()
                    .toEntity(byte[].class);

            // Copy back response
            response.setStatus(targetResponse.getStatusCode().value());
            targetResponse.getHeaders().forEach((name, values) -> {
                values.forEach(value -> response.addHeader(name, value));
            });

            if (targetResponse.getBody() != null) {
                response.getOutputStream().write(targetResponse.getBody());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("Routing error: " + e.getMessage());
        }
    }
}
