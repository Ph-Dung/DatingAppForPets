package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminPetItemResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String species;
    private String avatarUrl;
    private boolean hidden;
    private LocalDateTime createdAt;
}
