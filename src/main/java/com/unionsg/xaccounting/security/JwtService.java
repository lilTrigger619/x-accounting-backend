package com.unionsg.xaccounting.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // In production move these to application.properties / environment variables
    private final String SECRET         = "veryveryveryveryveryverysecretkey1234";
    private final String REFRESH_SECRET = "refreshsecretkeyverylongandverysecure5678";

    private Key getAccessSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private Key getRefreshSignKey() {
        return Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes());
    }

    // ── Access token — short lived (15 min) ───────────────────────────────────
    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 15)) // 15 min
                .signWith(getAccessSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Refresh token — long lived (7 days) ───────────────────────────────────
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7 days
                .signWith(getRefreshSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token, getAccessSignKey()).getSubject();
    }

    public String extractUserIdFromRefreshToken(String token) {
        return extractAllClaims(token, getRefreshSignKey()).getSubject();
    }

    public boolean isAccessTokenValid(String token) {
        try {
            extractAllClaims(token, getAccessSignKey());
            return true;
        } catch (JwtException e) {
            System.out.println(">>> JWT validation failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println(">>> Unexpected error: " + e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            extractAllClaims(token, getRefreshSignKey());
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // Keep old method name so JwtAuthenticationFilter doesn't break
    public boolean isTokenValid(String token) {
        return isAccessTokenValid(token);
    }

    private Claims extractAllClaims(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}