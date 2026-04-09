package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchRequestResponse {
    private Long id;
    private Long senderPetId;
    private String senderPetName;
    private String senderPetAvatarUrl;
    private Long receiverPetId;
    private String receiverPetName;
    private String receiverPetAvatarUrl;
    private String status;
    private Boolean isSuperLike;
    private LocalDateTime createdAt;
    /** true khi cả 2 phía đều accepted → cho phép mở conversation */
    private boolean canOpenConversation;
}