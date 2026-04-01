package com.petmatch.backend.controller;

import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // WebSocket endpoint to receive messages
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageDto messageDto) {
        // Save message to database
        MessageDto savedMessage = chatService.saveMessage(messageDto);
        
        // Broadcast the message to the specific user queue
        // Clients should subscribe to: /user/{userId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(savedMessage.getReceiverId()), 
                "/queue/messages", 
                savedMessage
        );
    }

    // REST endpoint to fetch chat history
    @GetMapping("/api/chat/history")
    public ResponseEntity<List<MessageDto>> getChatHistory(
            @RequestParam Long user1Id, 
            @RequestParam Long user2Id) {
            
        return ResponseEntity.ok(chatService.getChatHistory(user1Id, user2Id));
    }

    // WebSocket endpoint for WebRTC Signaling (Video/Audio Calls)
    @MessageMapping("/chat.signal")
    public void processSignal(@Payload com.petmatch.backend.dto.SignalingMessage signalingMessage) {
        // Forward the WebRTC signal (OFFER, ANSWER, ICE_CANDIDATE, HANG_UP) directly to the receiver
        // Clients should subscribe to: /user/{userId}/queue/signals
        messagingTemplate.convertAndSendToUser(
                String.valueOf(signalingMessage.getReceiverId()),
                "/queue/signals",
                signalingMessage
        );
    }
}
