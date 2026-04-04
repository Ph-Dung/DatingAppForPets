package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchRequestDto {
    @NotNull
    private Long receiverPetId;

    /** true = Super Like (giới hạn 1 lần/ngày) */
    private Boolean isSuperLike = false;
}