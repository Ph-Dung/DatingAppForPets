package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.ChatbotMessageRequest;
import com.petmatch.backend.dto.response.ChatbotResponse;
import com.petmatch.backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 🤖 API: Chatbot hỗ trợ ghép đôi (tìm kiếm thú cưng qua chat)
 * 
 * Endpoints:
 * - POST /api/chatbot/message - Gửi tin nhắn, nhận phản hồi + suggestions
 * 
 * Flow tương tác:
 * 1. Client gửi ChatbotMessageRequest với toàn bộ lịch sử hội thoại
 * 2. Backend gọi OpenAI API (GPT-4o-mini) với system prompt tiếng Việt
 * 3. AI parse intent:
 *    - Nếu chỉ Q&A: return reply text, isReadyToSuggest=false, suggestions=[]
 *    - Nếu có đủ thông tin: return JSON {action: SEARCH, filters} → backend gợi ý
 * 4. Return ChatbotResponse với reply + suggestions (nếu có)
 * 
 * Note: Client phải gửi lại toàn bộ chat history mỗi request (stateless, không lưu server)
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Gửi tin nhắn chat. Body gồm toàn bộ lịch sử hội thoại.
     * Client gửi lại toàn bộ messages mỗi lần (stateless).
     */
    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> sendMessage(
            @Valid @RequestBody ChatbotMessageRequest req) {
        return ResponseEntity.ok(chatbotService.processMessage(req.getMessages()));
    }
}
