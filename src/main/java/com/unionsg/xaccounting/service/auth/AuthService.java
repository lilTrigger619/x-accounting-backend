package com.unionsg.xaccounting.service.auth;

import com.unionsg.xaccounting.dto.auth.LoginRequest;
import com.unionsg.xaccounting.dto.auth.LoginResponse;
import com.unionsg.xaccounting.dto.auth.RoleResponse;
import com.unionsg.xaccounting.entity.RefreshToken;
import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.repository.RefreshTokenRepository;
import com.unionsg.xaccounting.repository.UserRepository;
import com.unionsg.xaccounting.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {

        User user = userRepository
                .findByEmailAndStatus(request.getEmail(), UserStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String userId = user.getId().toString();

        // Generate tokens
        String accessToken  = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);

        // Store refresh token in DB
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 7)) // 7 days
                .revoked(false)
                .build());

        // Send refresh token as httpOnly cookie (same as your Node.js version)
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 7); // 7 days in seconds
        // cookie.setSecure(true); // uncomment in production
        response.addCookie(cookie);

        // Build roles with their permissions
        List<RoleResponse> roles = user.getRoles() == null ? Collections.emptyList()
                : user.getRoles().stream()
                        .map(role -> RoleResponse.builder()
                                .name(role.getName())
                                .permissions(
                                    role.getPermissions() == null ? Collections.emptySet()
                                    : role.getPermissions().stream()
                                            .map(Permission::getName)
                                            .collect(Collectors.toSet())
                                )
                                .build())
                        .collect(Collectors.toList());

        // Direct permissions on the user only
        Set<String> directPermissions = user.getPermissions() == null ? Collections.emptySet()
                : user.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(userId)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .directPermissions(directPermissions)
                .build();
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        // Extract refresh token from cookie
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            // Revoke token in DB
            refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }

        // Clear the cookie regardless
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete it
        response.addCookie(cookie);
    }

    // ── REFRESH ACCESS TOKEN ──────────────────────────────────────────────────

    @Transactional
    public String refresh(HttpServletRequest request) {

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token missing");
        }

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token expired or invalid");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        String userId = jwtService.extractUserIdFromRefreshToken(refreshToken);
        return jwtService.generateAccessToken(userId);
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}