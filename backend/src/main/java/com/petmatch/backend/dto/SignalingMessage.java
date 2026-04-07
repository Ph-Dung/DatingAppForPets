package com.petmatch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {
    private Long senderId;
    private Long receiverId;
    
    // Type can be: OFFER, ANSWER, ICE_CANDIDATE, HANG_UP
    private String type; 
    
    // Contains JSON stringified SDP or ICE Candidate payload
    private String data;
}
