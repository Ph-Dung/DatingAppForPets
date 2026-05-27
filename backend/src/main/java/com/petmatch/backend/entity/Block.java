package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Chặn Người dùng (ngăn giao tiếp/hiển thị)
 * 
 * Mục đích: Cho phép user A chặn user B không thấy hồ sơ + gửi tin nhắn
 * Logic: Khi A chặn B → B bị lọc khỏi danh sách gợi ý của A
 *        B không thể gửi tin nhắn cho A (bị chặn)
 * BlockLevel: Tùy chỉnh phạm vi chặn (ALL=chặn hoàn toàn, MESSAGING_ONLY=không tin nhắn)
 */
@Entity 
@Table(name = "blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id","blocked_id"}))
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Block {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Người dùng thực hiện chặn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    User blocker;

    /** Người dùng bị chặn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    User blocked;

    /** Timestamp khi block được tạo (không thay đổi được) */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /** Mức độ chặn: ALL (chặn hoàn toàn), MESSAGING_ONLY (không tin nhắn), v.v. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    BlockLevel level = BlockLevel.ALL;
}
