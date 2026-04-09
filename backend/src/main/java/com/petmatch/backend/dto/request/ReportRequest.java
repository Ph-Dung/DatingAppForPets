package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotNull
    private Long targetId;        // petId hoặc userId

    @NotBlank
    private String targetType;    // "PET_PROFILE", "USER", v.v.

    @NotBlank
    private String reason;
}
