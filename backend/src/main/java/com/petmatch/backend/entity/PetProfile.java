package com.petmatch.backend.entity;

import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.enums.ReproductiveStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 🐾 PetProfile - Entity lưu thông tin hồ sơ thú cưng
 * 
 * Bảng: pet_profiles
 * 
 * Mối quan hệ:
 * - PetProfile 1-to-1 User (owner_id UNIQUE + NOT NULL)
 *   → Mỗi user chỉ có 1 pet profile
 *   → Khi xóa user → xóa pet profile tự động (cascade)
 * 
 * - PetProfile 1-to-Many PetPhoto (pet_id FK)
 *   → 1 pet có nhiều ảnh
 *   → Khi xóa pet → xóa tất cả ảnh
 * 
 * - PetProfile 1-to-Many PetVaccination (pet_id FK)
 *   → 1 pet có nhiều bản ghi vaccine
 * 
 * - PetProfile 1-to-Many MatchRequest (sender_pet_id/receiver_pet_id FK)
 *   → 1 pet có thể gửi & nhận nhiều yêu cầu
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
public class PetProfile {
    
    // ═════════════════════════════════════════════════════════════
    // 🔑 PRIMARY KEY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID hồ sơ pet duy nhất (PK)
     * Auto-increment từ database
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ═════════════════════════════════════════════════════════════
    // 🔗 FOREIGN KEY - OWNER RELATIONSHIP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * User chủ sở hữu pet (FK → users.id)
     * 
     * Ràng buộc:
     * - NOT NULL: Phải có owner
     * - UNIQUE: Mỗi user chỉ có 1 pet profile
     * - LAZY: Không tải User khi fetch PetProfile
     *         (cải thiện performance, tránh N+1 query)
     * 
     * Cascade: CascadeType.ALL
     * - Xóa user → xóa pet profile tự động
     * 
     * @JsonIgnore: Không serialize User vào JSON
     *              (tránh circular reference)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    User owner;

    // ═════════════════════════════════════════════════════════════
    // 🐾 PET BASIC INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tên thú cưng (VD: "Bông", "Miki", "Mèo Mẫu")
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc có tên
     * - MAX LENGTH: 100 ký tự
     */
    @Column(nullable = false, length = 100)
    String name;

    /**
     * Loài thú cưng (VD: "Chó", "Mèo", "Thỏ")
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc có loài
     * - MAX LENGTH: 50 ký tự
     */
    @Column(nullable = false, length = 50)
    String species;

    /**
     * Giống (VD: "Poodle", "Husky", "Mèo Anh", "Lai")
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn (có thể không biết giống)
     * - MAX LENGTH: 100 ký tự
     */
    @Column(length = 100)
    String breed;

    /**
     * Giới tính: MALE (đực) / FEMALE (cái) / OTHER (khác)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - TYPE: ENUM → lưu string (MALE/FEMALE/OTHER)
     * - DB COLUMN: VARCHAR(10)
     * 
     * Ảnh hưởng:
     * - Ảnh hưởng tới matching (có thể tìm cùng/khác giới)
     * - Ảnh hưởng tới reproductiveStatus (SPAYED/NEUTERED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    Gender gender;

    /**
     * Ngày sinh (format: yyyy-MM-dd)
     * VD: 2022-01-15
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * 
     * Tính toán:
     * - Age = hiện tại - dateOfBirth (tính bằng năm)
     * - Dùng để: filter search (1-3 tuổi, dưới 5 tuổi)
     * - LƯU Ý: Age không lưu trong DB, tính on-the-fly
     */
    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    /**
     * Cân nặng (kg)
     * VD: 4.5, 7.2, 15.8
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - PRECISION: 5 (tổng 5 chữ số)
     * - SCALE: 2 (2 chữ số thập phân)
     * - Phạm vi: -999.99 đến 999.99 kg
     */
    @Column(name = "weight_kg", precision = 5, scale = 2)
    BigDecimal weightKg;

    /**
     * Màu lông/mắu (VD: "Nâu", "Trắng", "Cam", "Vàng")
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - MAX LENGTH: 100 ký tự
     */
    @Column(length = 100)
    String color;

    /**
     * Kích thước: "small" / "medium" / "large"
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - MAX LENGTH: 20 ký tự
     * 
     * Gợi ý:
     * - small: < 5kg (Poodle nhỏ, Pom, Chihuahua)
     * - medium: 5-20kg (Corgi, Beagle, Schnauzer)
     * - large: > 20kg (Husky, Golden, Chó Đức)
     */
    @Column(length = 20)
    String size;

    // ═════════════════════════════════════════════════════════════
    // 💊 REPRODUCTIVE & HEALTH INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tình trạng sinh sản: INTACT / NEUTERED / SPAYED
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - TYPE: ENUM → VARCHAR(30)
     * 
     * Giá trị:
     * - INTACT: Còn nguyên, chưa triệt sản
     * - NEUTERED: Đã triệt sản (đực)
     * - SPAYED: Đã triệt sản (cái)
     * 
     * Ảnh hưởng:
     * - INTACT + lookingFor=BREEDING → tìm partner phối giống
     * - NEUTERED/SPAYED + lookingFor=BREEDING → không thể (mâu thuẫn)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reproductive_status", nullable = false, length = 30)
    ReproductiveStatus reproductiveStatus;

    /**
     * Đã tiêm vaccine chưa?
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = false)
     * - TYPE: BOOLEAN
     * 
     * Logic:
     * - false: Chưa tiêm (hoặc không ghi nhập)
     * - true: Đã tiêm ít nhất 1 lần
     * - Auto-update: Khi addVaccination() → set true
     * - Auto-update: Khi deleteAllVaccinations() → set false
     */
    @Column(name = "is_vaccinated", nullable = false)
    @Builder.Default
    Boolean isVaccinated = false;

    /**
     * Ngày tiêm vaccine gần nhất (yyyy-MM-dd)
     * VD: 2024-05-27
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * 
     * Auto-update:
     * - Khi addVaccination() → set = max(vaccinatedDate)
     * - Dùng để: Hiển thị "Đã tiêm cách đây X ngày"
     */
    @Column(name = "last_vaccine_date")
    LocalDate lastVaccineDate;

    /**
     * Tình trạng sức khỏe: HEALTHY / SICK / RECOVERING / CHRONIC
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = HEALTHY)
     * - TYPE: ENUM → VARCHAR(30)
     * 
     * Giá trị:
     * - HEALTHY: Khỏe mạnh, có thể ghép đôi
     * - SICK: Đang bệnh, tạm dừng ghép đôi
     * - RECOVERING: Đang hồi phục, chờ khỏi
     * - CHRONIC: Bệnh mãn tính, cần partner hiểu biết
     * 
     * Ảnh hưởng:
     * - SICK → Tự động ẩn khỏi search
     * - RECOVERING → Hiển thị nhưng filter ngoài mặc định
     * - Dùng để tìm kiếm, gợi ý
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 30)
    @Builder.Default
    HealthStatus healthStatus = HealthStatus.HEALTHY;

    /**
     * Ghi chú sức khỏe (TEXT)
     * 
     * Ví dụ:
     * - "Dị ứng với cá"
     * - "Bệnh tim, cần hạn chế vận động"
     * - "Hồi phục từ phẫu thuật, không chơi quá vui"
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - TYPE: TEXT (có thể dài)
     * 
     * Hiển thị:
     * - Trong PetProfileResponse
     * - Người khác xem trước khi match
     */
    @Column(name = "health_notes", columnDefinition = "TEXT")
    String healthNotes;

    // ═════════════════════════════════════════════════════════════
    // 🎯 PREFERENCES & MATCHING
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tính cách (JSON string): ["friendly", "playful", "lazy"]
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - TYPE: TEXT (JSON format)
     * - Không validate trong DB (validate ở application layer)
     * 
     * Ví dụ tags:
     * - friendly, active, lazy, protective, playful
     * - calm, energetic, stubborn, curious, gentle
     * 
     * Dùng để:
     * - Filter search (tìm pet có personality phù hợp)
     * - Hiển thị trong profile
     * - AI scoring (chatbot & smart suggestions)
     * 
     * Format: JSON array string
     * ["friendly","playful","lazy"]
     */
    @Column(name = "personality_tags", columnDefinition = "TEXT")
    String personalityTags;

    /**
     * Mục đích ghép đôi: BREEDING / FRIENDSHIP / PLAY
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - TYPE: ENUM → VARCHAR(30)
     * 
     * Giá trị:
     * - BREEDING: Phối giống (tìm partner để sinh con)
     * - FRIENDSHIP: Kết bạn (tìm partner thân thiết lâu dài)
     * - PLAY: Vui chơi (tìm partner để vui chơi tạm thời)
     * 
     * Logic:
     * - BREEDING + INTACT → tìm INTACT
     * - BREEDING + SPAYED/NEUTERED → không hợp lệ
     * - FRIENDSHIP → có thể tìm bất kỳ
     * - PLAY → có thể tìm bất kỳ
     * 
     * Dùng để: Filter search & gợi ý
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "looking_for", nullable = false, length = 30)
    LookingFor lookingFor;

    /**
     * Ghi chú thêm về pet (TEXT)
     * 
     * Ví dụ:
     * - "Pet yêu quý của gia đình"
     * - "Có kinh nghiệm phối giống"
     * - "Cần partner trầm tính, không quá vui"
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn
     * - TYPE: TEXT (có thể dài)
     */
    @Column(columnDefinition = "TEXT")
    String notes;

    // ═════════════════════════════════════════════════════════════
    // 👁️ VISIBILITY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Hồ sơ có bị ẩn khỏi search/suggestions không?
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = false)
     * - TYPE: BOOLEAN
     * 
     * Giá trị:
     * - false: Hiển thị bình thường (default)
     * - true: Ẩn khỏi search & suggestions
     * 
     * Dùng khi:
     * - Pet bị bệnh → tạm dừng ghép đôi
     * - User muốn tạm dừng ứng dụng
     * - Pet đã tìm thấy partner (không cần match nữa)
     * 
     * Endpoint: PATCH /api/pets/toggle-hidden → flip boolean
     * 
     * Filter: getSuggestions & search → loại trừ isHidden=true
     */
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    Boolean isHidden = false;

    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMPS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian tạo hồ sơ (auto-set)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - IMMUTABLE: updatable = false (không thay đổi sau tạo)
     * - AUTO-SET: @CreationTimestamp (Hibernate)
     * 
     * Format: ISO 8601 (2024-05-27T10:30:00)
     * 
     * Dùng để:
     * - Sắp xếp (newest pets first)
     * - Filter (pets created after date X)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /**
     * Thời gian cập nhật lần cuối
     * 
     * Ràng buộc:
     * - NULLABLE: Tuỳ chọn (null khi vừa tạo)
     * - AUTO-UPDATE: @UpdateTimestamp (Hibernate)
     * - Tự động update mỗi khi bất kỳ field nào thay đổi
     * 
     * Format: ISO 8601 (2024-05-27T11:45:30)
     * 
     * Dùng để:
     * - Biết khi nào hồ sơ được cập nhật gần đây
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // ═════════════════════════════════════════════════════════════
    // 🔗 RELATIONSHIPS (1-to-Many)
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Danh sách ảnh của pet
     * 
     * Mối quan hệ:
     * - 1 PetProfile có many PetPhoto
     * - Mapped by: "pet" (field trong PetPhoto)
     * - Cascade: DELETE (xóa pet → xóa tất cả ảnh)
     * - OrphanRemoval: true (xóa ảnh orphan)
     * 
     * @JsonIgnoreProperties: Không serialize pet vào ảnh
     *                        (tránh circular reference)
     * 
     * Default: Danh sách rỗng (new ArrayList<>())
     * 
     * Dùng để:
     * - Lưu trữ tất cả ảnh của pet
     * - Quản lý avatar (1 ảnh làm avatar)
     * - Hiển thị gallery trong profile
     */
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("pet")
    List<PetPhoto> photos = new ArrayList<>();

    /**
     * Danh sách bản ghi vaccine
     * 
     * Mối quan hệ:
     * - 1 PetProfile có many PetVaccination
     * - Mapped by: "pet" (field trong PetVaccination)
     * - Cascade: DELETE (xóa pet → xóa tất cả vaccine)
     * - OrphanRemoval: true (xóa vaccine orphan)
     * 
     * @JsonIgnoreProperties: Không serialize pet vào vaccine
     * 
     * Default: Danh sách rỗng (new ArrayList<>())
     * 
     * Dùng để:
     * - Lưu lịch sử tiêm vaccine
     * - Tính isVaccinated & lastVaccineDate
     * - Reminder khi vaccine sắp hết hạn
     * - Hiển thị trong health section
     */
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("pet")
    List<PetVaccination> vaccinations = new ArrayList<>();

    /**
     * Danh sách yêu cầu like đã gửi (outgoing requests)
     * 
     * Mối quan hệ:
     * - 1 PetProfile có many MatchRequest (sender side)
     * - Mapped by: "senderPet" (field trong MatchRequest)
     * - Cascade: DELETE (xóa pet → xóa tất cả requests)
     * 
     * @JsonIgnore: Không serialize vào JSON
     *             (tránh vô tận, vì MatchRequest cũng có pet)
     * 
     * Default: Danh sách rỗng (new ArrayList<>())
     * 
     * Dùng để:
     * - Query "Tôi đã like ai?" → getMySentRequests()
     * - Tìm mutual likes (A like B AND B like A)
     * - Kiểm tra xem đã like pet này chưa (tránh duplicate)
     */
    @OneToMany(mappedBy = "senderPet", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    List<MatchRequest> sentRequests = new ArrayList<>();

    /**
     * Danh sách yêu cầu like đã nhận (incoming requests)
     * 
     * Mối quan hệ:
     * - 1 PetProfile có many MatchRequest (receiver side)
     * - Mapped by: "receiverPet" (field trong MatchRequest)
     * - Cascade: DELETE (xóa pet → xóa tất cả requests)
     * 
     * @JsonIgnore: Không serialize vào JSON
     * 
     * Default: Danh sách rỗng (new ArrayList<>())
     * 
     * Dùng để:
     * - Query "Ai đã like mình?" → getWhoLikedMe()
     * - Hiển thị notification "Ai thích bạn"
     * - Tìm mutual likes
     * - Kiểm tra xem đã block người này chưa
     */
    @OneToMany(mappedBy = "receiverPet", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    List<MatchRequest> receivedRequests = new ArrayList<>();
}