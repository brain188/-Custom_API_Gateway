package com.brain.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private ProxyService proxyService;

    @Test
    void testExecuteRequest() {
        String method = "GET";
        String targetUrl = "http://localhost:8081";
        String path = "/api/users";
        Map<String, String> headers = Map.of("X-Test", "value");
        
        when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        byte[] expectedBody = "response content".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(expectedBody, HttpStatus.OK);
        when(responseSpec.toEntity(byte[].class)).thenReturn(responseEntity);
        
        ProxyService.CachedResponse response = proxyService.executeRequest(method, targetUrl, path, null, headers, false);
        
        assertNotNull(response);
        assertEquals(200, response.status());
        assertArrayEquals(expectedBody, response.body());
    }
}
