package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MatchRequestDto {
    @NotNull
    private UUID receiverPetId;
}