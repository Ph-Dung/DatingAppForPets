package com.petmatch.backend.dto.request;

import com.petmatch.backend.enums.ReportTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommunityReportRequest {
    @NotNull(message = "Thiếu targetId")
    private Long targetId;

    @NotNull(message = "Thiếu loại đối tượng báo cáo")
    private ReportTargetType targetType;

    @NotBlank(message = "Lý do báo cáo không được để trống")
    private String reason;

    // Frontend option: report and hide post from current user's feed
    private Boolean hidePost;
}
