package com.petmatch.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 🚫 BlockResponse - DTO trả về thông tin người bị chặn
 * 
 * Dùng trong:
 * - Endpoint: GET /api/interactions/blocks (danh sách "Người tôi chặn")
 * - Response từ InteractionService.getMyBlocks()
 * 
 * Cách hoạt động:
 * - User click "..." → "Chặn người dùng này"
 * - Backend tạo Block record + filter khỏi suggestions
 * - User xem danh sách chặn: GET /api/interactions/blocks
 * - Return BlockResponse[] với avatar + timestamp
 * 
 * Ví dụ:
 * {
 *   "id": 1,
 *   "blockedUserId": 456,
 *   "blockedUserName": "Anh Tuấn",
 *   "blockedUserAvatarUrl": "https://cloudinary.com/...",
 *   "createdAt": "2024-05-27T15:30:00"
 * }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockResponse {
    /** ID bản ghi chặn */
    private Long id;
    
    /** ID user bị chặn */
    private Long blockedUserId;
    
    /** Tên user bị chặn (từ User.name) */
    private String blockedUserName;
    
    /** Avatar URL của user bị chặn */
    private String blockedUserAvatarUrl;
    
    /** Thời gian chặn */
    private LocalDateTime createdAt;
}
