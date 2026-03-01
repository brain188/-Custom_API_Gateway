package com.brain.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final RestClient restClient;

    @Cacheable(value = "gateway-cache", key = "#targetUrl + #path", condition = "#isCacheable && #method == 'GET'")
    public CachedResponse executeRequest(String method, String targetUrl, String path, byte[] body, java.util.Map<String, String> headers, boolean isCacheable) {
        
        RestClient.RequestBodySpec requestSpec = restClient.method(HttpMethod.valueOf(method))
                .uri(targetUrl + path);

        headers.forEach((name, value) -> {
            if (!name.equalsIgnoreCase("host")) {
                requestSpec.header(name, value);
            }
        });

        if (body != null && body.length > 0) {
            requestSpec.body(body);
        }

        ResponseEntity<byte[]> response = requestSpec.retrieve().toEntity(byte[].class);
        
        return new CachedResponse(
                response.getStatusCode().value(),
                response.getHeaders().toSingleValueMap(),
                response.getBody()
        );
    }

    public record CachedResponse(int status, java.util.Map<String, String> headers, byte[] body) implements Serializable {}
}
