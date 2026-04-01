package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(updatable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    protected void onCreate() {
        blockedAt = LocalDateTime.now();
    }
}
