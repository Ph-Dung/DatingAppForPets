package com.petmatch.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ChatbotMessageRequest {
    private List<ChatMessageDto> messages;

    @Data
    public static class ChatMessageDto {
        private String role;    // "user" or "assistant"
        private String content;
    }
}
