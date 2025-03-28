package com.filesystem.demo.service;

import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        jwtService = new JwtService(jwtUtil);
    }

    @Test
    void testExtractToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer my.jwt.token");

        String token = jwtService.extractToken(request);

        assertEquals("my.jwt.token", token);
    }

    @Test
    void testExtractTokenReturnsNullWhenNoHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        String token = jwtService.extractToken(request);

        assertNull(token);
    }

    @Test
    void testExtractUserMetadata() {
        String token = "token123";

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(jwtUtil.extractUsername(token)).thenReturn("testuser");
        when(jwtUtil.extractCompany(token)).thenReturn("TestCorp");
        when(jwtUtil.extractRole(token)).thenReturn("admin");

        UserMetadata metadata = jwtService.extractUserMetadata(token);

        assertNotNull(metadata);
        assertEquals("user123", metadata.getUserId());
        assertEquals("testuser", metadata.getUsername());
        assertEquals("TestCorp", metadata.getCompany());
        assertEquals("admin", metadata.getRole());
    }

    @Test
    void testExtractUserMetadataReturnsNullWhenTokenIsNull() {
        UserMetadata metadata = jwtService.extractUserMetadata(null);
        assertNull(metadata);
    }
}
