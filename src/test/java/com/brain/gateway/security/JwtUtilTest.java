package com.brain.gateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "my-very-long-secret-key-that-is-at-least-thirty-two-characters-long";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", expiration);
    }

    @Test
    void testGenerateAndValidateToken() {
        String username = "testuser";
        List<String> roles = List.of("ROLE_USER");
        
        String token = jwtUtil.generateToken(username, roles);
        assertNotNull(token);
        
        assertTrue(jwtUtil.validateToken(token, username));
        assertEquals(username, jwtUtil.extractUsername(token));
        
        List<String> extractedRoles = jwtUtil.extractRoles(token);
        assertEquals(roles, extractedRoles);
    }

    @Test
    void testInvalidToken() {
        // Jwts.parser().parse() will throw an exception for malformed tokens
        String invalidToken = "invalid.token.here";
        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken, "user"));
    }

    @Test
    void testExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Set to past
        String token = jwtUtil.generateToken("user", List.of("ROLE_USER"));
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtUtil.validateToken(token, "user"));
    }
}
