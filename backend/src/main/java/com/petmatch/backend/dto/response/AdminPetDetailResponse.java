package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminPetDetailResponse {
    private PetProfileResponse pet;
    private List<AdminReportItemResponse> violations;
}