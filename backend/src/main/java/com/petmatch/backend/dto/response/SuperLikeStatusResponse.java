package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 🌟 SuperLikeStatusResponse - DTO trả về trạng thái Super Like quota
 * 
 * Dùng trong:
 * - Endpoint: GET /api/matches/super-like-status → kiểm tra quota
 * 
 * Cấu trúc:
 * {
 *   "canSuperLike": true,
 *   "nextResetAt": "2024-05-28T00:00:00",
 *   "usedToday": 0
 * }
 * 
 * 📋 Quy luật Super Like:
 * - Mỗi pet được super like 1 lần/ngày
 * - Quota reset lúc 00:00 hôm sau
 * - Không giới hạn regular like
 * - Super like = cơ hội cao hơn được nhìn thấy
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class SuperLikeStatusResponse {
    /**
     * Còn được super like hôm nay không?
     * 
     * - true: Chưa dùng, có thể gửi super like bây giờ
     * - false: Đã dùng hôm nay, phải chờ đến ngày mai
     */
    private boolean canSuperLike;
    
    /**
     * Thời điểm quota được reset
     * 
     * Format: ISO 8601 (2024-05-28T00:00:00)
     * 
     * Ví dụ:
     * - Nếu hôm nay 2024-05-27 → nextResetAt = 2024-05-28T00:00:00
     * - Nếu hôm nay 2024-05-28 → nextResetAt = 2024-05-29T00:00:00
     * 
     * Dùng để: UI hiển thị "Quay lại vào lúc XYZ để được super like lại"
     */
    private LocalDateTime nextResetAt;
    
    /**
     * Số super like đã dùng hôm nay
     * 
     * - 0: Chưa dùng
     * - 1: Đã dùng 1 lần (hôm nay)
     * 
     * Logic: Chỉ có 0 hoặc 1 (quota = 1/ngày)
     */
    private int usedToday;
}
