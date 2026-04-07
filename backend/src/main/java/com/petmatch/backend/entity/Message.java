package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(updatable = false)
    private LocalDateTime sentAt;

    /** Người nhận đã đọc tin nhắn này chưa — dùng để hiển thị badge chưa đọc */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
    }
}

