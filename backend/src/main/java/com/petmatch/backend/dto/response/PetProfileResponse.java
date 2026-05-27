package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 🐾 PetProfileResponse - DTO trả về thông tin hồ sơ thú cưng đầy đủ
 * 
 * Dùng trong:
 * - Tất cả API liên quan đến pet (create, get, suggestions, search)
 * - Response từ PetProfileController
 * - Suggestions từ chatbot
 * 
 * Cấu trúc dữ liệu gồm:
 * 1. ℹ️ BASIC INFO: id, name, owner info
 * 2. 🐾 PET INFO: species, breed, gender, age, weight...
 * 3. 💊 HEALTH: reproductive status, vaccine info, health status
 * 4. 🎯 PREFERENCES: personality, looking for, notes
 * 5. 👁️ VISIBILITY: isHidden
 * 6. 📸 PHOTOS: avatar + all photos
 * 7. 🗺️ LOCATION: distance, owner address
 * 8. ⏱️ TIMESTAMPS: created date
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class PetProfileResponse {
    // ═════════════════════════════════════════════════════════════
    // ℹ️ BASIC INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID hồ sơ pet duy nhất
     */
    private Long id;
    
    /**
     * ID chủ sở hữu (User)
     */
    private Long ownerId;
    
    /**
     * Tên chủ sở hữu
     */
    private String ownerName;
    
    // ═════════════════════════════════════════════════════════════
    // 🐾 PET INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tên thú cưng (VD: "Bông", "Miki")
     */
    private String name;
    
    /**
     * Loài thú cưng (VD: "Chó", "Mèo", "Thỏ")
     */
    private String species;
    
    /**
     * Giống (VD: "Poodle", "Husky", "Mèo Anh")
     */
    private String breed;
    
    /**
     * Giới tính: MALE/FEMALE/OTHER
     */
    private String gender;
    
    /**
     * Ngày sinh (format: yyyy-MM-dd)
     */
    private LocalDate dateOfBirth;
    
    /**
     * Tuổi tính bằng năm (auto-calculate từ dateOfBirth)
     * VD: dateOfBirth = 2022-01-15 → age = 2 (năm 2024)
     */
    private Integer age;
    
    /**
     * Cân nặng (kg)
     * VD: 4.5, 7.2, 15.8
     */
    private BigDecimal weightKg;
    
    /**
     * Màu lông (VD: "Nâu", "Trắng", "Cam")
     */
    private String color;
    
    /**
     * Kích thước: "small", "medium", "large"
     */
    private String size;
    
    // ═════════════════════════════════════════════════════════════
    // 💊 HEALTH INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tình trạng sinh sản: INTACT/NEUTERED/SPAYED
     * - INTACT: Còn nguyên
     * - NEUTERED: Đã triệt sản (đực)
     * - SPAYED: Đã triệt sản (cái)
     */
    private String reproductiveStatus;
    
    /**
     * Đã tiêm vaccine chưa?
     */
    private Boolean isVaccinated;
    
    /**
     * Ngày tiêm vaccine gần nhất
     */
    private LocalDate lastVaccineDate;
    
    /**
     * Tổng số lần tiêm vaccine đã ghi nhập
     * Auto-calculate từ PetVaccination records count
     */
    private Integer vaccinationCount;
    
    /**
     * Tình trạng sức khỏe: HEALTHY/SICK/RECOVERING/CHRONIC
     * - HEALTHY: Khỏe mạnh
     * - SICK: Đang bệnh
     * - RECOVERING: Đang hồi phục
     * - CHRONIC: Bệnh mãn tính
     */
    private String healthStatus;
    
    /**
     * Ghi chú sức khỏe (VD: "Dị ứng với một số loại thức ăn")
     */
    private String healthNotes;
    
    // ═════════════════════════════════════════════════════════════
    // 🎯 PREFERENCES & MATCHING
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tính cách (JSON format): ["friendly", "active", "lazy"]
     * Các tag phổ biến: friendly, active, lazy, protective, playful...
     */
    private String personalityTags;
    
    /**
     * Mục đích ghép đôi: BREEDING/FRIENDSHIP/PLAY
     * - BREEDING: Phối giống
     * - FRIENDSHIP: Kết bạn
     * - PLAY: Vui chơi
     */
    private String lookingFor;
    
    /**
     * Ghi chú thêm về pet
     */
    private String notes;
    
    // ═════════════════════════════════════════════════════════════
    // 👁️ VISIBILITY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Hồ sơ có bị ẩn khỏi search/suggestions không?
     * - true: Ẩn (không hiển thị cho người khác)
     * - false: Hiển thị bình thường
     * 
     * Dùng khi: pet bị bệnh, không muốn ghép đôi, ...
     */
    private Boolean isHidden;
    
    // ═════════════════════════════════════════════════════════════
    // 📸 PHOTOS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * URL ảnh đại diện (avatar)
     * Dùng để hiển thị trong card suggestions/search
     */
    private String avatarUrl;
    
    /**
     * Danh sách URL tất cả ảnh của pet
     * Dùng để hiển thị gallery
     */
    private List<String> photoUrls;
    
    /**
     * Danh sách chi tiết ảnh (gồm id + url)
     * Dùng khi cần quản lý từng ảnh
     * VD: [{"id": 1, "url": "..."}, {"id": 2, "url": "..."}]
     */
    private List<PetPhotoDto> photos;
    
    // ═════════════════════════════════════════════════════════════
    // 🗺️ LOCATION INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Khoảng cách từ user hiện tại (km)
     * - null: User chưa có GPS hoặc chưa enable location
     * - 0.5: 500 mét
     * - 5.2: 5.2 km
     * 
     * Tính toán: Haversine formula dựa trên GPS của 2 user
     */
    private Double distanceKm;
    
    /**
     * Địa chỉ văn bản của chủ sở hữu
     * VD: "Quận 1, TP. HCM"
     */
    private String ownerAddress;
    
    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMPS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian tạo hồ sơ
     * Auto-set lúc create, không thay đổi
     */
    private LocalDateTime createdAt;
}
