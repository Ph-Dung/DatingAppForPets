package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 📸 PetPhoto - Entity lưu thông tin ảnh thú cưng
 * 
 * Bảng: pet_photos
 * 
 * Mối quan hệ:
 * - PetProfile 1-to-Many PetPhoto
 * - Mỗi pet có thể có nhiều ảnh (0..N)
 * - Mỗi pet phải có ít nhất 1 ảnh làm avatar
 * - Khi xóa pet → xóa tất cả ảnh
 * 
 * Lưu trữ ảnh:
 * - Không lưu file trong database
 * - Chỉ lưu URL từ Cloudinary
 * - Cloudinary quản lý upload, transform, delete
 * 
 * Avatar logic:
 * - Mỗi pet có 1 ảnh avatar (isAvatar=true)
 * - Avatar dùng để hiển thị trong card suggestions/search
 * - Khi delete avatar → chọn ảnh next làm avatar
 * - Nếu last photo → error (pet phải có ít nhất 1 ảnh)
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
public class PetPhoto {

    // ═════════════════════════════════════════════════════════════
    // 🔑 PRIMARY KEY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID ảnh duy nhất
     * Auto-increment từ database
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ═════════════════════════════════════════════════════════════
    // 🔗 FOREIGN KEY - PET RELATIONSHIP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Pet sở hữu ảnh (FK → pet_profiles.id)
     * 
     * Ràng buộc:
     * - NOT NULL: Phải gắn với pet
     * - LAZY: Không tải Pet khi fetch Photo
     * 
     * Cascade: CascadeType.ALL
     * - Xóa pet → xóa tất cả ảnh
     * 
     * Mapped by: @OneToMany(mappedBy="pet") ở PetProfile
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    PetProfile pet;

    // ═════════════════════════════════════════════════════════════
    // 🖼️ PHOTO INFORMATION
    // ═════════════════════════════════════════════════════════════
    
    /**
     * URL ảnh lưu trên Cloudinary
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - TYPE: TEXT (URL dài)
     * 
     * Format: https://res.cloudinary.com/...
     * 
     * Ví dụ:
     * - https://res.cloudinary.com/petmatch/image/upload/v1716796200/pets/abc123.jpg
     * - Cloudinary sẽ auto-generate variations (thumbnails, crops)
     * - Có thể append transformations: ?w=300&h=300&c=fill
     * 
     * Lưu ý:
     * - Không lưu local file path
     * - Không lưu binary data
     * - URL được trả về sau khi upload CloudinaryService
     * - Nếu Cloudinary delete → URL sẽ 404
     * 
     * Dùng để:
     * - Hiển thị trong UI
     * - QR code scan (public URL)
     * - API response
     */
    @Column(name = "photo_url", nullable = false, columnDefinition = "TEXT")
    String photoUrl;

    /**
     * Đây có phải ảnh đại diện (avatar) không?
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = false)
     * - TYPE: BOOLEAN
     * 
     * Giá trị:
     * - true: Avatar (ảnh đại diện chính)
     * - false: Ảnh phụ (gallery)
     * 
     * Logic:
     * - Mỗi pet chỉ có 1 avatar (isAvatar=true)
     * - Nhận diện: SELECT * FROM pet_photos WHERE pet_id=? AND is_avatar=true (1 row)
     * - Khi upload ảnh đầu tiên → auto set isAvatar=true
     * - Khi delete avatar → find next photo (createdAt ASC) → set as avatar
     * - Nếu delete last photo → error (pet cần ít nhất 1 ảnh)
     * 
     * Hiển thị:
     * - Avatar → PetProfileResponse.avatarUrl
     * - Gallery → PetProfileResponse.photoUrls[] & photos[]
     * - Suggestions card → hiển thị avatar
     * 
     * Endpoint:
     * - POST /api/pets/photos?setAsAvatar=true → upload & set avatar
     * - PUT /api/pets/photos/{photoId} → cập nhật isAvatar
     * - DELETE /api/pets/photos/{photoId} → xóa & chọn avatar mới
     * 
     * @Query cho avatar:
     * - findByPetAndIsAvatarTrue()
     * - findByPetIdAndIsAvatarTrue(petId)
     * - select count(*) from pet_photos where pet_id=? and is_avatar=true (for validation)
     */
    @Builder.Default
    @Column(name = "is_avatar", nullable = false)
    Boolean isAvatar = false;

    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMP
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian upload ảnh (auto-set)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - IMMUTABLE: updatable = false (không thay đổi)
     * - AUTO-SET: @CreationTimestamp (Hibernate)
     * 
     * Format: ISO 8601 (2024-05-27T10:30:00)
     * 
     * Dùng để:
     * - Sắp xếp ảnh (newest/oldest first)
     * - Chọn avatar khi delete (chọn next oldest)
     * - Timeline: "Uploaded X days ago"
     * - Query: Photos uploaded after date
     * 
     * Sắp xếp logic:
     * - Avatar selection (delete avatar): ORDER BY uploadedAt ASC LIMIT 1
     * - Gallery display: ORDER BY uploadedAt DESC (newest first)
     * - Age: now() - uploadedAt
     */
    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    LocalDateTime uploadedAt;

    // ═════════════════════════════════════════════════════════════
    // 📌 DATABASE INDEX & CONSTRAINTS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Index: (pet_id, is_avatar)
     * 
     * Dùng để:
     * - Fast query: findByPetIdAndIsAvatarTrue()
     * - SELECT * FROM pet_photos WHERE pet_id=123 AND is_avatar=true
     * - Performance: O(log n) thay vì O(n)
     * 
     * Giải thích:
     * - Mỗi pet có nhiều ảnh nhưng chỉ 1 avatar
     * - Index giúp tìm avatar nhanh
     * - Thường query khi: load profile, delete photo
     * 
     * Tạo index:
     * CREATE INDEX idx_pet_photos_pet_id_is_avatar 
     * ON pet_photos(pet_id, is_avatar);
     */
}
