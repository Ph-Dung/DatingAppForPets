package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ghép nối Chat cấp User (riêng biệt với MatchRequest cấp pet)
 * 
 * Mục đích: Cho phép nhắn tin/gọi điện giữa 2 người dùng
 * Tạo khi: Cả 2 MatchRequest (cấp pet) trở thành ACCEPTED
 * Dùng cho: Danh sách chat, lịch sử tin nhắn, chức năng gọi điện
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người dùng thứ nhất trong ghép nối (quan hệ M-N) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /** Người dùng thứ hai trong ghép nối (quan hệ M-N) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    /** Timestamp khi ghép nối được tạo (không thay đổi được) */
    @Column(updatable = false)
    private LocalDateTime matchedAt;

    /** Tự động đặt matchedAt = now() khi tạo */
    @PrePersist
    protected void onCreate() {
        matchedAt = LocalDateTime.now();
    }
}
