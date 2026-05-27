package com.petmatch.backend.entity;

import com.petmatch.backend.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ❤️ MatchRequest - Entity lưu yêu cầu ghép đôi (Pet-level)
 * 
 * Bảng: match_requests
 * 
 * Khái niệm quan trọng:
 * MatchRequest là ở PET LEVEL (pet thích pet)
 * Match là ở USER LEVEL (user chat với user)
 * 
 * Flow:
 * 1️⃣ Pet A gửi like đến Pet B
 *    → Tạo MatchRequest(A→B, status=PENDING)
 * 
 * 2️⃣ Kiểm tra xem Pet B có đã like Pet A trước không
 *    → Nếu YES: Auto-match → status=ACCEPTED + tạo Match record
 *    → Nếu NO: Chờ Pet B phản hồi
 * 
 * 3️⃣ Pet B phản hồi:
 *    ✅ ACCEPT → status=ACCEPTED → tạo Match (có thể chat)
 *    ❌ REJECT → status=REJECTED → end
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Entity
@Table(name = "match_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_pet_id","receiver_pet_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchRequest {

    // ═════════════════════════════════════════════════════════════
    // 🔑 PRIMARY KEY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * ID yêu cầu ghép đôi duy nhất
     * Auto-increment từ database
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ═════════════════════════════════════════════════════════════
    // 🔗 FOREIGN KEYS - RELATIONSHIP TO PETS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Pet gửi like (FK → pet_profiles.id)
     * 
     * Ràng buộc:
     * - NOT NULL: Phải có sender
     * - LAZY: Không tải pet khi fetch MatchRequest
     * 
     * Cascade: CascadeType.ALL
     * - Xóa pet → xóa tất cả requests của pet này (sender)
     * 
     * Ví dụ:
     * - Pet A (Bông) like Pet B (Miki)
     * - senderPet = A (Bông)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_pet_id", nullable = false)
    PetProfile senderPet;

    /**
     * Pet nhận like (FK → pet_profiles.id)
     * 
     * Ràng buộc:
     * - NOT NULL: Phải có receiver
     * - LAZY: Không tải pet khi fetch MatchRequest
     * 
     * Cascade: CascadeType.ALL
     * - Xóa pet → xóa tất cả requests của pet này (receiver)
     * 
     * Ví dụ:
     * - Pet A (Bông) like Pet B (Miki)
     * - receiverPet = B (Miki)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_pet_id", nullable = false)
    PetProfile receiverPet;

    // ═════════════════════════════════════════════════════════════
    // 📊 MATCH STATUS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Trạng thái yêu cầu: PENDING / ACCEPTED / REJECTED
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = PENDING)
     * - TYPE: ENUM → VARCHAR(20)
     * 
     * Giá trị:
     * - PENDING: A đã like B, chờ B phản hồi
     *   → Ngoại lệ: Nếu B đã like A trước → auto ACCEPTED
     *   → B sẽ thấy thông báo "Ai thích bạn" (có avatar A)
     * 
     * - ACCEPTED: Cả A và B đã đồng ý
     *   → Có thể mở conversation
     *   → Tạo Match record (user-level)
     *   → Có thể chat, video call
     * 
     * - REJECTED: B đã từ chối hoặc A hủy request
     *   → Không thể match nữa
     *   → LƯU Ý: Tạo Dislike record để không suggest lại
     * 
     * Transition:
     * PENDING → ACCEPTED (khi respond với status=ACCEPTED)
     * PENDING → REJECTED (khi respond với status=REJECTED)
     * PENDING → ACCEPTED (auto, nếu mutual)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    MatchStatus status = MatchStatus.PENDING;

    // ═════════════════════════════════════════════════════════════
    // 🌟 SUPER LIKE FLAG
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Có phải Super Like không?
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc (default = false)
     * - TYPE: BOOLEAN
     * 
     * Giá trị:
     * - false: Like thường (unlimited)
     * - true: Super Like (quota: 1/ngày/pet)
     * 
     * Logic:
     * - Khi gửi like với isSuperLike=true:
     *   1. Check quota: `countBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(today)`
     *   2. Nếu count >= 1 → reject (quota exceeded)
     *   3. Nếu count = 0 → accept & create MatchRequest
     * 
     * - Super Like = chuyển đến top trong "Liked You" list
     * - Người nhận sẽ thấy Super Like indicator (⭐)
     * 
     * Ưu điểm:
     * - Tăng cơ hội được nhìn thấy
     * - Giúp pet phổ biến khác thấy bạn
     * - Thể hiện sự quan tâm đặc biệt
     */
    @Builder.Default
    @Column(name = "is_super_like", nullable = false)
    Boolean isSuperLike = false;

    // ═════════════════════════════════════════════════════════════
    // ⏱️ TIMESTAMPS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Thời gian gửi like (auto-set)
     * 
     * Ràng buộc:
     * - NOT NULL: Bắt buộc
     * - IMMUTABLE: updatable = false (không thay đổi)
     * - AUTO-SET: @CreationTimestamp (Hibernate)
     * 
     * Format: ISO 8601 (2024-05-27T10:30:00)
     * 
     * Dùng để:
     * - Sắp xếp danh sách "Ai thích mình" (newest first)
     * - Quota check: Super like hôm nay? → compareTo(startOfDay)
     * - Filter: Requests từ ngày X đến Y
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
     * - Tự động update khi status/isSuperLike thay đổi
     * 
     * Format: ISO 8601 (2024-05-27T11:45:30)
     * 
     * Dùng để:
     * - Biết khi nào request được phản hồi
     * - Audit trail (khi nào trạng thái thay đổi)
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // ═════════════════════════════════════════════════════════════
    // 📌 DATABASE CONSTRAINTS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * @UniqueConstraint
     * Tên: UNIQUE(sender_pet_id, receiver_pet_id)
     * 
     * Ý nghĩa:
     * - Mỗi cặp (A→B) chỉ có 1 MatchRequest
     * - Pet A không thể like Pet B 2 lần
     * - Nếu A like B lần 1 → A không like B lần 2
     *   (phải chờ B phản hồi hoặc A hủy)
     * 
     * Lợi ích:
     * - Tránh duplicate requests
     * - Tiết kiệm storage
     * - Giảm notifications spam
     * 
     * Lưu ý:
     * - (A→B) khác (B→A)
     * - A có thể like B, B cũng có thể like A
     * - Mutual: (A→B, status=ACCEPTED) + (B→A, status=ACCEPTED)
     */
}