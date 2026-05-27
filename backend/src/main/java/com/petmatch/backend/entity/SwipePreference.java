package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu trữ sở thích AI từ các "thích" của người dùng
 * 
 * Mục đích: Xây dựng mô hình sở thích từ các lượt thích/không thích để "gợi ý thông minh"
 * Cập nhật: Sau mỗi lượt thích/không thích bởi AiMatchingService.updatePreferences()
 * Dùng cho: Tính điểm AI khi có ≥5 lượt thích để xếp hạng ứng viên
 */
@Entity
@Table(name = "swipe_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwipePreference {

    /** 1-1 với PetProfile (không cần FK, PK dùng làm FK) */
    @Id
    Long petId;

    /** Mảng JSON: ["Poodle","Golden Retriever","Husky"] - học từ các lượt thích */
    @Column(name = "preferred_breeds", columnDefinition = "TEXT")
    String preferredBreeds;

    /** NAM/CÁI/null - giới tính trung bình của thú cưng được thích */
    @Column(name = "preferred_gender", length = 10)
    String preferredGender;

    /** Cân nặng trung bình (kg) của thú cưng được thích */
    @Column(name = "avg_weight")
    Double avgWeight;

    /** Tuổi trung bình (năm) của thú cưng được thích */
    @Column(name = "avg_age")
    Double avgAge;

    /** true nếu user thích thú cưng khỏe mạnh (từ mẫu lượt thích) */
    @Column(name = "prefer_healthy")
    Boolean preferHealthy;

    /** PHỐI_GIỐNG/KẾT_BẠN/VUI_CHƠI/null - mục đích ưa thích từ lượt thích */
    @Column(name = "preferred_looking_for", length = 20)
    String preferredLookingFor;

    /** Tổng số lượt thích gửi bởi thú cưng này (bộ đếm) */
    @Column(name = "total_likes")
    int totalLikes;

    /** Tự động cập nhật khi sở thích thay đổi (thích/không thích) */
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
