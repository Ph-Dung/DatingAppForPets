package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 🚨 ReportRequest - DTO request báo cáo lạm dụng/spam
 * 
 * Dùng trong:
 * - Endpoint: POST /api/reports (báo cáo user hoặc hồ sơ pet)
 * - Trang ghép đôi: click "..." → "Báo cáo hồ sơ"
 * 
 * Logic báo cáo:
 * 1. Tạo Report record với status = PENDING (chờ duyệt)
 * 2. Admin review: xem reason + target info
 * 3. Admin xử lý: set action (WARN/SUSPEND/BAN) + status (RESOLVED)
 * 4. Track thống kê báo cáo (dashboard admin)
 * 
 * Ví dụ:
 * POST /api/reports
 * { "targetId": 123, "targetType": "USER", "reason": "Profile giả mạo" }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
public class ReportRequest {
    /** ID mục tiêu: userId hoặc petId */
    @NotNull
    private Long targetId;

    /** Loại báo cáo: USER, PET_PROFILE */
    @NotBlank
    private String targetType;

    /** Lý do báo cáo (TEXT): "Profile giả mạo", "Spam tin nhắn", "Ảnh không phù hợp" */
    @NotBlank
    private String reason;
}
