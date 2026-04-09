package com.petmatch.backend.dto;

import com.petmatch.backend.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isRead;
    /** TEXT / IMAGE / VOICE */
    private MessageType type;
    /** URL media nếu type != TEXT */
    private String mediaUrl;
}
