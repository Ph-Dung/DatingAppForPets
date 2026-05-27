package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ❤️ MatchRequestResponse - DTO trả về thông tin yêu cầu ghép đôi
 * 
 * Dùng trong:
 * - Endpoint: GET /api/matches/sent → danh sách gửi đi
 * - Endpoint: GET /api/matches/received → danh sách nhận
 * - Endpoint: GET /api/matches/matched → danh sách match thành công
 * - Endpoint: POST /api/matches → gửi like/super like
 * 
 * Cấu trúc:
 * {
 *   "id": 789,
 *   "senderPetId": 111,
 *   "senderPetName": "Bông",
 *   "senderPetAvatarUrl": "https://cloudinary.com/...",
 *   "receiverPetId": 222,
 *   "receiverPetName": "Miki",
 *   "receiverPetAvatarUrl": "https://cloudinary.com/...",
 *   "status": "ACCEPTED",
 *   "isSuperLike": true,
 *   "canOpenConversation": true,
 *   "createdAt": "2024-05-27T10:30:00"
 * }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class MatchRequestResponse {
    // ═════════════════════════════════════════════════════════════
    // 🔑 KEY FIELDS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID match request duy nhất
     */
    private Long id;
    
    // ═════════════════════════════════════════════════════════════
    // 🐾 SENDER (Người gửi like)
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID thú cưng gửi like
     */
    private Long senderPetId;
    
    /**
     * Tên thú cưng gửi like
     */
    private String senderPetName;
    
    /**
     * URL ảnh đại diện của sender
     */
    private String senderPetAvatarUrl;
    
    // ═════════════════════════════════════════════════════════════
    // 🐾 RECEIVER (Người nhận like)
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID thú cưng nhận like
     */
    private Long receiverPetId;
    
    /**
     * Tên thú cưng nhận like
     */
    private String receiverPetName;
    
    /**
     * URL ảnh đại diện của receiver
     */
    private String receiverPetAvatarUrl;
    
    // ═════════════════════════════════════════════════════════════
    // 📊 MATCH STATUS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Trạng thái của request:
     * - "PENDING": Chờ receiver phản hồi
     * - "ACCEPTED": Đã accept, có thể chat
     * - "REJECTED": Đã từ chối
     * 
     * LƯU Ý: Auto-match (Tinder-style)
     * Nếu A like B, và B đã like A trước → auto ACCEPTED
     */
    private String status;
    
    /**
     * Có phải Super Like không?
     * - true: Super Like (quota: 1/ngày/pet)
     * - false: Regular Like (unlimited)
     */
    private Boolean isSuperLike;
    
    // ═════════════════════════════════════════════════════════════
    // 💬 CHAT PERMISSION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Có thể mở conversation không?
     * - true: Status = ACCEPTED & Mutual = true → Cho phép chat/call
     * - false: Status = PENDING hoặc 1 chiều → Chưa được chat
     * 
     * Luật:
     * A like B → status = PENDING → canOpenConversation = false
     * B like A trước → auto ACCEPTED → canOpenConversation = true
     */
    private boolean canOpenConversation;
    
    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian gửi like
     */
    private LocalDateTime createdAt;
}