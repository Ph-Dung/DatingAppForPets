package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 💉 PetVaccination - Entity lưu bản ghi lịch sử tiêm vaccine
 * 
 * Bảng: pet_vaccinations
 * 
 * Mối quan hệ:
 * - PetProfile 1-to-Many PetVaccination
 * - Mỗi pet có thể có nhiều bản ghi vaccine
 * - Khi xóa pet → xóa tất cả vaccine records
 * 
 * Dùng để:
 * - Lưu lịch sử vaccine
 * - Auto-update isVaccinated & lastVaccineDate ở PetProfile
 * - Nhắc nhở khi vaccine sắp hết hạn
 * - Hiển thị health history
 * 
 * Ví dụ:
 * Pet "Bông" có vaccine history:
 * 1. 2024-05-27: Dại (next due: 2025-05-27)
 * 2. 2024-05-27: 5in1 (next due: 2024-08-27)
 * 3. 2024-04-15: Lepto (no next due = suốt đời)
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetVaccination {

    // ═════════════════════════════════════════════════════════════
    // 🔑 PRIMARY KEY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID bản ghi vaccine duy nhất
     * Auto-increment từ database
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ═════════════════════════════════════════════════════════════
    // 🔗 FOREIGN KEY - PET RELATIONSHIP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Pet được tiêm vaccine (FK → pet_profiles.id)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc có pet
     * - LAZY: Không tải Pet khi fetch Vaccination
     * 
     * Cascade: CascadeType.ALL
     * - Xóa pet → xóa tất cả vaccine records
     * 
     * Mapped by: @OneToMany(mappedBy="pet") ở PetProfile
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    PetProfile pet;

    // ═════════════════════════════════════════════════════════════
    // 💉 VACCINE INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tên vaccine được tiêm
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - MAX LENGTH: 100 ký tự
     * 
     * Ví dụ:
     * - "Dại" (Rabies)
     * - "5in1" (DHPP: Distemper, Hepatitis, Parvovirus, Parainfluenza)
     * - "Lepto" (Leptospirosis)
     * - "Viêm gan" (Hepatitis)
     * - "Uốn ván" (Tetanus)
     * - "Bordetella" (Kennel Cough)
     * - "Lyme" (Lyme disease)
     */
    @Column(name = "vaccine_name", nullable = false, length = 100)
    String vaccineName;

    /**
     * Ngày tiêm vaccine (format: yyyy-MM-dd)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * 
     * Ví dụ: 2024-05-27
     * 
     * Dùng để:
     * - Lịch sử: "Đã tiêm vaccine X vào ngày Y"
     * - Auto-update PetProfile.lastVaccineDate = max(vaccinatedDate)
     * - Filter: Vaccines from date X to Y
     * - Calculation: Cách bây giờ bao lâu?
     */
    @Column(name = "vaccinated_date", nullable = false)
    LocalDate vaccinatedDate;

    /**
     * Ngày cần tiêm lại (next due date)
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - Nếu null: Vaccine suốt đời, không cần tiêm lại
     * 
     * Ví dụ:
     * - vaccinatedDate=2024-05-27, nextDueDate=2025-05-27 (1 năm)
     * - vaccinatedDate=2024-05-27, nextDueDate=2024-08-27 (3 tháng)
     * - vaccinatedDate=2024-05-27, nextDueDate=null (suốt đời)
     * 
     * Dùng để:
     * - Nhắc nhở user: "Vaccine sắp hết hạn!"
     * - Push notification khi hôm nay = nextDueDate
     * - Dashboard: Hiển thị "Vaccines due soon"
     * - Statistic: "X% pets có vaccine hết hạn"
     */
    @Column(name = "next_due_date")
    LocalDate nextDueDate;

    /**
     * Tên phòng khám / bệnh viện
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - MAX LENGTH: 150 ký tự
     * 
     * Ví dụ:
     * - "Phòng khám thú cưng ABC"
     * - "Bệnh viện động vật Mèo Mẫu Chó"
     * - "Veterinary Clinic XYZ"
     * - "Pet Hospital Quận 1"
     * 
     * Dùng để:
     * - Tham khảo: "Nơi tiêm vaccine này là đâu?"
     * - Giới thiệu phòng khám: "Nơi này tiêm vaccine tốt"
     * - Review: "Phòng khám này có vấn đề"
     */
    @Column(name = "clinic_name", length = 150)
    String clinicName;

    /**
     * Ghi chú thêm từ bác sĩ/người tư vấn
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - TYPE: TEXT (có thể dài)
     * 
     * Ví dụ:
     * - "Tiêm thành công, không có phản ứng"
     * - "Pet cần tái tiêm sau 3 tuần"
     * - "Dễ dàng, pet không hứng thú"
     * - "Có sốt nhẹ sau tiêm, đã hết"
     * - "Bác sĩ khuyên tăng cường miễn dịch"
     * - "Tuổi vàng để tiêm, hôm nay sẽ tiêm lại"
     * 
     * Dùng để:
     * - Lưu trữ thông tin quan trọng
     * - Giúp next visit (bác sĩ biết history)
     * - Medical tracking
     */
    @Column(columnDefinition = "TEXT")
    String notes;

    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian ghi nhập bản ghi vào database (auto-set)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - IMMUTABLE: updatable = false (không thay đổi)
     * - AUTO-SET: @CreationTimestamp (Hibernate)
     * 
     * Format: ISO 8601 (2024-05-27T10:30:00)
     * 
     * LƯU Ý:
     * - createdAt: Khi ghi nhập vào hệ thống (có thể muộn)
     * - vaccinatedDate: Khi thực tế tiêm (ở phòng khám)
     * - Có thể: createdAt > vaccinatedDate (ghi nhập muộn)
     * 
     * Ví dụ:
     * - vaccinatedDate = 2024-05-20 (tiêm hôm đó)
     * - createdAt = 2024-05-27 (user ghi nhập hôm nay)
     * - Gap = 7 ngày (user quên ghi nhập)
     * 
     * Dùng để:
     * - Audit trail: Khi nào user ghi nhập record
     * - Sort: Newest records first
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    // ═════════════════════════════════════════════════════════════
    // 🔄 AUTO-UPDATE LOGIC
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Khi addVaccination() trong PetProfileService:
     * 1. Tạo PetVaccination record
     * 2. Auto-update PetProfile:
     *    - isVaccinated = true
     *    - lastVaccineDate = max(vaccinatedDate from all records)
     * 
     * Khi deleteVaccination():
     * 1. Xóa PetVaccination record
     * 2. Nếu không còn vaccine nào:
     *    - isVaccinated = false
     *    - lastVaccineDate = null
     * 3. Nếu còn vaccine khác:
     *    - lastVaccineDate = max(vaccinatedDate from remaining)
     * 
     * @Query nên sử dụng:
     * - SELECT MAX(v.vaccinatedDate) FROM PetVaccination v WHERE v.pet.id = ?
     * - ORDER BY nextDueDate ASC NULLS LAST (to find overdue)
     */
}