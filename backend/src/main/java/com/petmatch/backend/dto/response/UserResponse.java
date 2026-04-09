package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private boolean warned;
    private int warningCount;
    private LocalDateTime lastWarnedAt;
    private String warningMessage;
    private LocalDateTime createdAt;
}
