package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Report;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Tra cứu báo cáo lạm dụng/spam (kiểm duyệt)
 * 
 * Dùng bởi: InteractionService.submitReport()
 * Dùng bởi: Bảng điều khiển admin để xem xét báo cáo
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU TÌM KIẾM BÁO CÁO
    // ════════════════════════════════════════════════════════════════
    
    /** Get all reports submitted by user (sorted newest first) */
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    
    /** Get all reports for target object (e.g., all reports for userId=123) */
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType, Long targetId);
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU TRẠNG THÁI (KIỂM DUYỆT ADMIN)
    // ════════════════════════════════════════════════════════════════
    
    /** Get all reports with status (PENDING/RESOLVED/DISMISSED) */
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    
    /** Count reports by status (for admin dashboard metrics) */
    long countByStatus(ReportStatus status);
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU PHÂN TRANG (BẢNG ĐIỀU KHIỂN ADMIN)
    // ════════════════════════════════════════════════════════════════
    
    /** All reports paginated (newest first) */
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /** Reports by status paginated (for admin workflow) */
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}
