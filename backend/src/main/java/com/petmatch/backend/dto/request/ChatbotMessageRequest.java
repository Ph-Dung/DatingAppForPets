package com.petmatch.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ChatbotMessageRequest {
    @NotEmpty(message = "messages must not be empty")
    @Valid
    private List<ChatMessageDto> messages;

    @Data
    public static class ChatMessageDto {
        @NotBlank(message = "role is required")
        private String role;    // "user" or "assistant"
        @NotBlank(message = "content is required")
        private String content;
    }
}
