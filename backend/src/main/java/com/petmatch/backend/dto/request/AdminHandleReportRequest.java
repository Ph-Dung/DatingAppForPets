package com.petmatch.backend.dto.request;

import com.petmatch.backend.enums.AdminReportAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminHandleReportRequest {
    @NotNull
    private AdminReportAction action;

    private String note;
}
