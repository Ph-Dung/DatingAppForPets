package com.petmatch.backend.entity;

import com.petmatch.backend.enums.AdminReportAction;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
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
    ReportTargetType targetType;   // USER, POST, COMMENT, PET_PROFILE

    @Column(name = "target_id", nullable = false)
    Long targetId;                 // ID của đối tượng bị report (petId hoặc userId)

    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    User handledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 30)
    AdminReportAction action;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    String adminNote;

    @Column(name = "handled_at")
    LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}