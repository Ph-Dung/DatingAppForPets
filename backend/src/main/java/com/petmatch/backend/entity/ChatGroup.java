package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhóm chat — hỗ trợ nhiều người (>2) cùng nhắn tin.
 */
@Entity
@Table(name = "chat_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** URL ảnh đại diện của nhóm (tuỳ chọn) */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Người tạo nhóm */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Danh sách thành viên nhóm */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatGroupMember> members = new ArrayList<>();
}
