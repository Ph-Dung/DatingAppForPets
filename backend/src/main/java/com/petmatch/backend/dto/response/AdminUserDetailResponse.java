package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminUserDetailResponse {
    private AdminUserItemResponse user;
    private List<AdminReportItemResponse> violations;
}