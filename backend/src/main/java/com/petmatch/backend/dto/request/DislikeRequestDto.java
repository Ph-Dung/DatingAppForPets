package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DislikeRequestDto {
    @NotNull
    private Long dislikedPetId;
}
