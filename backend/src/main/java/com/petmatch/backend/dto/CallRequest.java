package com.petmatch.backend.dto;

import com.petmatch.backend.entity.CallType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRequest {
    @NotNull(message = "calleeId is required")
    private Long calleeId;

    @NotNull(message = "type is required")
    private CallType type; // AUDIO or VIDEO
}
