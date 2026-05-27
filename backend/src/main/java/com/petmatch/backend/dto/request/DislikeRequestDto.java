package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 👎 DislikeRequestDto - DTO request bỏ qua/không thích thú cưng
 * 
 * Dùng trong:
 * - Endpoint: POST /api/matches/{receiverPetId}/dislike
 * - Request từ trang ghép đôi (user click "Bỏ qua")
 * 
 * Logic:
 * - Tạo Dislike record (dislikerPet → dislikedPet)
 * - Ngăn chặn gợi ý thú cưng này cho user trong tương lai
 * - Tracked để cải thiện AI suggestions
 * 
 * Ví dụ:
 * POST /api/matches/222/dislike
 * { "dislikedPetId": 222 }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
public class DislikeRequestDto {
    /** ID thú cưng bị user không thích (tránh gợi ý lại) */
    @NotNull
    private Long dislikedPetId;
}
