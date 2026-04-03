package com.petmatch.backend.entity;

import com.petmatch.backend.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Report {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    ReportTargetType targetType;   // POST, COMMENT, USER

    @Column(name = "target_id", nullable = false)
    UUID targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;

    @Column(nullable = false, length = 20)
    String status = "PENDING";     // PENDING, RESOLVED, DISMISSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}