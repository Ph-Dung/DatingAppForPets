package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO: Request gửi like hoặc super like
 * - POST /api/matches
 * 
 * Logic: 
 * - Kiểm tra không self-like, không duplicate
 * - Nếu mutual → auto-match (tạo Match record)
 */
@Data
public class MatchRequestDto {
    @NotNull
    private Long receiverPetId;  // Pet bị like

    // false = like thường (unlimited)
    // true = super like (quota: 1 lần/ngày, reset 00:00)
    private Boolean isSuperLike = false;
}