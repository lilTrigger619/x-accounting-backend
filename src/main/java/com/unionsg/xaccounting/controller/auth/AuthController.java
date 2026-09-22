package com.unionsg.xaccounting.controller.auth;

import com.unionsg.xaccounting.dto.auth.CurrentUserResponse;
import com.unionsg.xaccounting.dto.auth.LoginRequest;
import com.unionsg.xaccounting.dto.auth.LoginResponse;
import com.unionsg.xaccounting.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    // POST /api/auth/me/photo
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurrentUserResponse> uploadPhoto(@RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(authService.uploadPhoto(photo));
    }

    // DELETE /api/auth/me/photo
    @DeleteMapping("/me/photo")
    public ResponseEntity<CurrentUserResponse> deletePhoto() {
        return ResponseEntity.ok(authService.deletePhoto());
    }
}