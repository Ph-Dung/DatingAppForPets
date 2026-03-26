package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private UUID userId;
    private boolean hasPetProfile;
}