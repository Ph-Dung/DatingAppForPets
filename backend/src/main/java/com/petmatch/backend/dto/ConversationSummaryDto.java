package com.petmatch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {
    private Long matchedUserId;
    private String userName;
    private String avatarUrl;
    private String lastMessage;
    private String lastMessageTime;   // ISO-8601 string
    private long unreadCount;
    private boolean isOnline;
    private boolean isMuted;
}
