package com.petmatch.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageResponse {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private LocalDateTime sentAt;
}
