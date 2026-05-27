package com.petmatch.backend.dto.request;

import com.petmatch.backend.enums.AdminReportAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 👨‍💼 AdminHandleReportRequest - DTO request admin xử lý báo cáo
 * 
 * Dùng trong:
 * - Endpoint: PUT /api/admin/reports/{reportId} (xử lý báo cáo)
 * - Admin dashboard: review báo cáo → chọn hành động
 * 
 * Quy trình admin moderation:
 * 1. Admin view báo cáo pending: GET /api/admin/reports?status=PENDING
 * 2. Admin xem chi tiết: targetType, targetId, reason
 * 3. Admin quyết định hành động:
 *    - WARN: cảnh báo user (keep account)
 *    - SUSPEND: tạm khóa account (7-30 ngày)
 *    - BAN: vĩnh viễn khóa account
 *    - DELETE_CONTENT: xóa nội dung spam
 * 4. POST request kèm action + note → Report.status = RESOLVED
 * 
 * Ví dụ:
 * PUT /api/admin/reports/101
 * { "action": "SUSPEND", "note": "Lạm dụng tin nhắn. Tạm khóa 14 ngày" }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
public class AdminHandleReportRequest {
    /** Hành động: WARN/SUSPEND/BAN/DELETE_CONTENT */
    @NotNull
    private AdminReportAction action;

    /** Ghi chú admin (VD: "Lạm dụng tin nhắn. Kiểm soát 2 tuần") */
    private String note;
}
