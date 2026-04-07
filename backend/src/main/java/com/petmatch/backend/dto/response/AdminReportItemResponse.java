package com.petmatch.backend.dto.response;

import com.petmatch.backend.enums.AdminReportAction;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReportItemResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private Long handledById;
    private String handledByName;
    private AdminReportAction action;
    private String adminNote;
    private LocalDateTime handledAt;
}
