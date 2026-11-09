package com.healthcare.claimsmanagement.controller;

import com.healthcare.claimsmanagement.dto.*;
import com.healthcare.claimsmanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(Authentication authentication,
                                                   @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring("Bearer ".length()));
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @GetMapping("/me")
    public UserProfile me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
