package com.petmatch.backend.controller;

import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * WebSocket endpoint nhận tin nhắn.
     * senderId được lấy từ principal của WebSocket session (đã xác thực qua JWT ở CONNECT),
     * KHÔNG tin từ payload của client để tránh giả mạo.
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageDto messageDto,
                            org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor) {
        // Fix #2: Lấy senderId từ WebSocket principal (đã authenticate bằng JWT)
        String email = headerAccessor.getUser() != null
                ? headerAccessor.getUser().getName()
                : null;
        if (email == null) throw new IllegalStateException("Unauthenticated WebSocket connection");

        Long senderId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email))
                .getId();

        // Override senderId từ JWT, không tin client
        messageDto.setSenderId(senderId);

        MessageDto savedMessage = chatService.saveMessage(messageDto);

        // Gửi tới người nhận: /user/{receiverId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(savedMessage.getReceiverId()),
                "/queue/messages",
                savedMessage
        );
        // Gửi lại cho chính người gửi để confirm
        messagingTemplate.convertAndSendToUser(
                String.valueOf(savedMessage.getSenderId()),
                "/queue/messages.sent",
                savedMessage
        );
    }

    /**
     * WebSocket endpoint WebRTC Signaling (gọi video/audio).
     * receiverId được giữ nguyên vì chỉ dùng để routing — không ảnh hưởng bảo mật.
     */
    @MessageMapping("/chat.signal")
    public void processSignal(@Payload com.petmatch.backend.dto.SignalingMessage signalingMessage) {
        // Forward WebRTC signal (OFFER, ANSWER, ICE_CANDIDATE, HANG_UP, INCOMING_CALL) tới người nhận
        messagingTemplate.convertAndSendToUser(
                String.valueOf(signalingMessage.getReceiverId()),
                "/queue/signals",
                signalingMessage
        );
    }

    /**
     * Fix #12: Lấy lịch sử chat với phân trang — mặc định page=0, size=30.
     * GET /api/chat/history?user1Id=1&user2Id=2&page=0&size=30
     */
    @GetMapping("/history")
    public ResponseEntity<List<MessageDto>> getChatHistory(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(chatService.getChatHistory(user1Id, user2Id, page, size));
    }

    /** Đánh dấu đã đọc toàn bộ tin nhắn từ senderId gửi cho receiverId */
    @PutMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            Authentication auth) {
        // Chỉ người nhận mới được mark as read
        chatService.markAsRead(senderId, receiverId);
        return ResponseEntity.noContent().build();
    }

    /** Đếm số tin nhắn chưa đọc */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @RequestParam Long senderId,
            @RequestParam Long receiverId) {
        long count = chatService.countUnread(senderId, receiverId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}

