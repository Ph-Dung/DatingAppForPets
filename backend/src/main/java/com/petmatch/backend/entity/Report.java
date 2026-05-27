package com.petmatch.backend.entity;

import com.petmatch.backend.enums.AdminReportAction;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Báo Cáo Lạm Dụng/Spam (quy trình kiểm duyệt)
 * 
 * Mục đích: Theo dõi báo cáo từ user để admin xem xét
 * Luồng: PENDING → (admin xem xét) → RESOLVED/DISMISSED + hành động thực hiện
 * Hành động: CẢNH_BÁO (cảnh báo), TẠM_KHÓA (cấm tạm thời), KHÓA_VĩNH_VIỄN (cấm vĩnh viễn), XÓA (xóa nội dung)
 * Mục tiêu: USER, POST, COMMENT, PET_PROFILE
 */
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

    /** Người dùng gửi báo cáo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    User reporter;

    /** Đối tượng bị báo cáo: USER, POST, COMMENT, PET_PROFILE */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    ReportTargetType targetType;

    /** ID của đối tượng bị báo cáo (userId, petId, postId, v.v.) */
    @Column(name = "target_id", nullable = false)
    Long targetId;

    /** Lý do/mô tả báo cáo */
    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;

    /** Trạng thái: PENDING (chờ admin xem), RESOLVED, DISMISSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    ReportStatus status = ReportStatus.PENDING;

    /** Admin xử lý báo cáo (null cho đến khi được xử lý) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    User handledBy;

    /** Hành động thực hiện: CẢNH_BÁO, TẠM_KHÓA, KHÓA, XÓA_NỘI_DUNG */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 30)
    AdminReportAction action;

    /** Ghi chú của admin/lý do quyết định */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    String adminNote;

    /** Timestamp khi admin xử lý báo cáo */
    @Column(name = "handled_at")
    LocalDateTime handledAt;

    /** Timestamp khi báo cáo được tạo (không thay đổi được) */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}