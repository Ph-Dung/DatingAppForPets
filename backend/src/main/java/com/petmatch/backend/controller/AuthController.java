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
    /**
     * Controller xử lý xác thực người dùng (auth).
     * Bao gồm các endpoint: đăng ký, đăng nhập người dùng thường và đăng nhập admin.
     * Trả về `AuthResponse` chứa token JWT và thông tin cơ bản của user.
     */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req) {
        // Đăng ký tài khoản mới, trả về token và thông tin người dùng
        return ResponseEntity.status(201).body(authService.register(req));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req) {
        // Đăng nhập user bình thường
        return ResponseEntity.ok(authService.login(req));
    }
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(
            @Valid @RequestBody LoginRequest req) {
        // Đăng nhập admin (kiểm tra role trước khi trả token admin)
        return ResponseEntity.ok(authService.loginAdmin(req));
    }
}
