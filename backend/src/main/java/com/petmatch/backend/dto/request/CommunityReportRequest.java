package com.petmatch.backend.dto.request;

import com.petmatch.backend.enums.ReportTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO: Yêu cầu gửi báo cáo nội dung (report) từ user.
 * - `targetType` chỉ chấp nhận POST hoặc COMMENT theo quy định hiện tại.
 * - `hidePost` là tuỳ chọn phía frontend cho phép ẩn bài viết này khỏi feed của người gửi report.
 */
@Data
public class CommunityReportRequest {
    @NotNull(message = "Thiếu targetId")
    private Long targetId; // ID của đối tượng bị report (postId hoặc commentId)

    @NotNull(message = "Thiếu loại đối tượng báo cáo")
    private ReportTargetType targetType; // Loại đối tượng bị report (POST hoặc COMMENT)

    @NotBlank(message = "Lý do báo cáo không được để trống")
    private String reason; // Mô tả lý do report

    // Frontend option: report and hide post from current user's feed
    private Boolean hidePost; // Nếu true và targetType==POST thì ẩn bài cho reporter
}
