package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MatchRequestResponse {
    private UUID id;
    private UUID senderPetId;
    private String senderPetName;
    private String senderPetAvatarUrl;
    private UUID receiverPetId;
    private String receiverPetName;
    private String status;
    private LocalDateTime createdAt;
    // true khi cả 2 phía đều accepted → cho phép mở conversation
    private boolean canOpenConversation;
}