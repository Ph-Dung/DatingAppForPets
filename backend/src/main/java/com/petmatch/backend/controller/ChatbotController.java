package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.ChatbotMessageRequest;
import com.petmatch.backend.dto.response.ChatbotResponse;
import com.petmatch.backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
