package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private Long userId;
    private boolean hasPetProfile;
}