package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.LoginRequest;
import com.petmatch.backend.dto.request.RegisterRequest;
import com.petmatch.backend.dto.response.AuthResponse;
import com.petmatch.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(201).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.loginAdmin(req));
    }
}
