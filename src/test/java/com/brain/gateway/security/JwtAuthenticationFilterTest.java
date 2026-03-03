package com.brain.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void testFilterWithValidToken() throws Exception {
        String token = "valid-token";
        String username = "testuser";
        List<String> roles = List.of("ROLE_USER");
        
        lenient().when(request.getServletPath()).thenReturn("/api/data");
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        lenient().when(jwtUtil.extractUsername(token)).thenReturn(username);
        lenient().when(jwtUtil.validateToken(token, username)).thenReturn(true);
        lenient().when(jwtUtil.extractRoles(token)).thenReturn(roles);
        
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFilterWithInvalidToken() throws Exception {
        String token = "invalid-token";
        String username = "testuser";
        
        lenient().when(request.getServletPath()).thenReturn("/api/data");
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        lenient().when(jwtUtil.extractUsername(token)).thenReturn(username);
        lenient().when(jwtUtil.validateToken(token, username)).thenReturn(false);
        
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilterWithNoToken() throws Exception {
        lenient().when(request.getServletPath()).thenReturn("/api/data");
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
