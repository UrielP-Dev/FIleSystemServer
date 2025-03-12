
package com.filesystem.demo.service;

import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.utils.JwtUtil;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;


@Service
public class JwtService {

    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
    }

    public UserMetadata extractUserMetadata(String token) {
        if (token == null) return null;
        return new UserMetadata(
                jwtUtil.extractUserId(token),
                jwtUtil.extractUsername(token),
                jwtUtil.extractCompany(token),
                jwtUtil.extractRole(token)
        );
    }
}
