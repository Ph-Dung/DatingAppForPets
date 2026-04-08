package com.petmatch.backend.dto.response;

import java.time.LocalDateTime;

import com.petmatch.backend.enums.CommunityNotificationType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityNotificationResponse {
    Long id;
    CommunityNotificationType type;
    String message;
    Long postId;
    Long commentId;
    Long actorId;
    String actorName;
    String actorAvatar;
    Boolean isRead;
    LocalDateTime createdAt;
}
