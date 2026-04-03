package com.petmatch.backend.dto;

import com.petmatch.backend.entity.CallType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRequest {
    private Long calleeId;
    private CallType type; // AUDIO or VIDEO
}
