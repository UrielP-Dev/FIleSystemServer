package com.filesystem.demo.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String token;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("mysecretkeymysecretkeymysecretkeymysecretkey".getBytes());
        long expiration = 1000 * 60 * 60; // 1 hour
        jwtUtil = new JwtUtil(secret, expiration);
        token = jwtUtil.generateToken("user1", "admin", "123", "AcmeCorp");
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        String invalidToken = token + "tampered";
        assertFalse(jwtUtil.isTokenValid(invalidToken));
    }

    @Test
    void testExpiredTokenIsInvalid() throws InterruptedException {
        String shortLivedSecret = Base64.getEncoder().encodeToString("short-lived-key-1234567890123456".getBytes());
        JwtUtil shortLivedJwtUtil = new JwtUtil(shortLivedSecret, 1); // 1 ms
        String shortToken = shortLivedJwtUtil.generateToken("user2", "user", "456", "ShortCorp");
        Thread.sleep(10);
        assertFalse(shortLivedJwtUtil.isTokenValid(shortToken));
    }

    @Test
    void testMalformedTokenThrowsException() {
        String malformed = "malformed.token.without.structure";
        assertFalse(jwtUtil.isTokenValid(malformed));
    }

    @Test
    void testNullTokenIsInvalid() {
        assertFalse(jwtUtil.isTokenValid(null));
    }

    @Test
    void testGenerateAndExtractClaimsSuccessfully() {
        String token = jwtUtil.generateToken("johndoe", "admin", "123", "AcmeCorp");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("johndoe", jwtUtil.extractUsername(token));
        assertEquals("admin", jwtUtil.extractRole(token));
        assertEquals("123", jwtUtil.extractUserId(token));
        assertEquals("AcmeCorp", jwtUtil.extractCompany(token));
    }
}
