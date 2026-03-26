package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.LoginRequest;
import com.petmatch.backend.dto.request.RegisterRequest;
import com.petmatch.backend.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest req);

    AuthResponse login(LoginRequest req);
}
