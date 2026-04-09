package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    User blocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    BlockLevel level = BlockLevel.ALL;
}
