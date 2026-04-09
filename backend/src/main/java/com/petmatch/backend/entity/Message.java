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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    /** URL ảnh hoặc file voice (null nếu type = TEXT) */
    @Column(columnDefinition = "TEXT")
    private String mediaUrl;

    /** Nội dung văn bản (null nếu type = IMAGE hoặc VOICE) */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(updatable = false)
    private LocalDateTime sentAt;

    /** Người nhận đã đọc tin nhắn này chưa */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /** Soft-delete: thời điểm sender xóa conversation này */
    private LocalDateTime deletedBySenderAt;

    /** Soft-delete: thời điểm receiver xóa conversation này */
    private LocalDateTime deletedByReceiverAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
        if (type == null) type = MessageType.TEXT;
    }
}
