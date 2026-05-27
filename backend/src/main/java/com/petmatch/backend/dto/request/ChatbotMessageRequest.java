package com.petmatch.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 🤖 ChatbotMessageRequest - DTO request chat với AI để tìm thú cưng phù hợp
 * 
 * Dùng trong:
 * - Endpoint: POST /api/chatbot/message (interactive chat)
 * - User: "Tôi muốn tìm chó cái từ 1-3 tuổi"
 * 
 * Flow chatbot AI:
 * 1. Client gửi message list (chat history) tới backend
 * 2. Backend gọi OpenAI API (GPT-4o-mini) với system prompt tiếng Việt
 * 3. AI parse intent: (a) chỉ trả lời, hoặc (b) action=SEARCH với filters
 * 4. Backend thực thi search + trả lại ChatbotResponse với suggestions
 * 
 * Ví dụ request:
 * POST /api/chatbot/message
 * {
 *   "messages": [
 *     { "role": "user", "content": "Tôi muốn tìm chó cái" },
 *     { "role": "assistant", "content": "Bạn muốn loại chó nào?" },
 *     { "role": "user", "content": "Poodle từ 1-3 tuổi, cân nặng 3-5kg" }
 *   ]
 * }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
public class ChatbotMessageRequest {
    /** Lịch sử chat: [user msg, assistant reply, user msg, ...] */
    @NotEmpty(message = "messages must not be empty")
    @Valid
    private List<ChatMessageDto> messages;

    /**
     * Một tin nhắn trong cuộc thoại
     */
    @Data
    public static class ChatMessageDto {
        /** "user" hoặc "assistant" */
        @NotBlank(message = "role is required")
        private String role;
        
        /** Nội dung tin nhắn (tiếng Việt) */
        @NotBlank(message = "content is required")
        private String content;
    }
}
