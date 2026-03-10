package com.unionsg.xaccounting.controller.auth;

import org.springframework.web.bind.annotation.*;
import com.unionsg.xaccounting.dto.auth.LoginResponse;
import com.unionsg.xaccounting.dto.auth.LoginRequest;
import com.unionsg.xaccounting.service.auth.AuthService;

@RestController
@RequestMapping("/api/auth")
//@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
//
//    @PostMapping("/register")
//    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
//        authService.register(request);
//        return ResponseEntity.ok("User registered successfully");
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request){
//        return ResponseEntity.ok(authService.login(request));
//    }

    public AuthController(AuthService authService){
        this.authService  = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }
}
