package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 💉 VaccinationResponse - DTO trả về thông tin bản ghi vaccine
 * 
 * Dùng trong:
 * - Endpoint: GET /api/pets/vaccinations → danh sách vaccine
 * - Endpoint: POST /api/pets/vaccinations → thêm vaccine
 * - Endpoint: PUT /api/pets/vaccinations/{vacId} → cập nhật vaccine
 * - Response từ PetProfileService.addVaccination()
 * 
 * Cấu trúc:
 * {
 *   "id": 555,
 *   "vaccineName": "Dại",
 *   "vaccinatedDate": "2024-05-27",
 *   "nextDueDate": "2025-05-27",
 *   "clinicName": "Phòng khám thú cưng ABC",
 *   "notes": "Tiêm thành công, không có phản ứng",
 *   "createdAt": "2024-05-27T10:30:00"
 * }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class VaccinationResponse {
    // ═════════════════════════════════════════════════════════════
    // 🔑 KEY FIELD
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID bản ghi vaccine duy nhất
     */
    private Long id;
    
    // ═════════════════════════════════════════════════════════════
    // 💉 VACCINE INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tên vaccine được tiêm
     * Ví dụ: "Dại", "5in1", "Lepto", "Viêm gan", "Uốn ván"
     */
    private String vaccineName;
    
    /**
     * Ngày tiêm vaccine (format: yyyy-MM-dd)
     * Ví dụ: "2024-05-27"
     */
    private LocalDate vaccinatedDate;
    
    /**
     * Ngày cần tiêm lại (vaccine mũi tiếp theo)
     * - Nếu null: Không cần tiêm lại (vaccine suốt đời)
     * - Ví dụ: "2025-05-27" (năm tiêm tiếp)
     * 
     * Dùng để: Nhắc nhở user khi hết hạn
     */
    private LocalDate nextDueDate;
    
    /**
     * Tên phòng khám / bệnh viện tiêm
     * Ví dụ: "Phòng khám thú cưng Mèo Mẫu Chó"
     */
    private String clinicName;
    
    /**
     * Ghi chú từ bác sĩ/người tư vấn
     * Ví dụ: "Tiêm thành công, không có phản ứng"
     *       "Pet cần tái tiêm sau 3 tuần"
     */
    private String notes;
    
    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian ghi nhập bản ghi vaccine vào database
     * Auto-set lúc create, không thay đổi
     */
    private LocalDateTime createdAt;
}
