package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Không Thích cấp Pet/Bỏ qua (ngăn không gợi ý lại)
 * 
 * Mục đích: Theo dõi những thú cưng user đã bỏ qua/không thích
 * Logic: Khi pet A không thích pet B → B không bao giờ gợi ý cho A lại
 * Khác với: Block (cấp pet vs cấp user)
 * Ràng buộc: UNIQUE(disliker_pet_id, disliked_pet_id) - không trùng lặp
 */
@Entity
@Table(name = "dislikes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"disliker_pet_id","disliked_pet_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dislike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Thú cưng không thích/bỏ qua */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disliker_pet_id", nullable = false)
    PetProfile dislikerPet;

    /** Thú cưng bị không thích/bỏ qua */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disliked_pet_id", nullable = false)
    PetProfile dislikedPet;

    /** Timestamp khi bỏ qua được ghi lại (không thay đổi được) */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
