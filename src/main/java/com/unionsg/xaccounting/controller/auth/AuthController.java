package com.unionsg.xaccounting.controller.auth;

import com.unionsg.xaccounting.dto.auth.LoginRequest;
import com.unionsg.xaccounting.dto.auth.LoginResponse;
import com.unionsg.xaccounting.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        
        this.authService = authService;
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of(
            "status", "200",
            "message", "Logged out successfully"
        ));
    }

    // POST /api/auth/refresh
    // Client calls this when access token expires — gets a new access token using the httpOnly cookie
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletRequest request) {
        String newAccessToken = authService.refresh(request);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}