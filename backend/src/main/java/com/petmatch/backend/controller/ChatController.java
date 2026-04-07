package com.petmatch.backend.controller;

import com.petmatch.backend.dto.ConversationSummaryDto;
import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepository userRepository;

    // ── WebSocket: Gửi tin nhắn ──────────────────────────────────────────────

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageDto messageDto,
                            org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor) {
        String email = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
        if (email == null) throw new IllegalStateException("Unauthenticated WebSocket connection");

        Long senderId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email))
                .getId();
        messageDto.setSenderId(senderId);

        MessageDto savedMessage = chatService.saveMessage(messageDto);

        // Gửi tới người nhận
        messagingTemplate.convertAndSendToUser(
                String.valueOf(savedMessage.getReceiverId()),
                "/queue/messages",
                savedMessage
        );
        // Confirm cho người gửi
        messagingTemplate.convertAndSendToUser(
                String.valueOf(savedMessage.getSenderId()),
                "/queue/messages.sent",
                savedMessage
        );
    }

    // ── WebSocket: WebRTC Signaling ──────────────────────────────────────────

    @MessageMapping("/chat.signal")
    public void processSignal(@Payload com.petmatch.backend.dto.SignalingMessage signalingMessage) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(signalingMessage.getReceiverId()),
                "/queue/signals",
                signalingMessage
        );
    }

    // ── REST: Danh sách conversation (match-gated) ───────────────────────────

    /**
     * GET /api/chat/conversations
     * Trả danh sách các cuộc trò chuyện của user hiện tại,
     * chỉ gồm người đã match.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations() {
        return ResponseEntity.ok(chatService.getConversations());
    }

    // ── REST: Lịch sử chat ───────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<List<MessageDto>> getChatHistory(
            @RequestParam Long user1Id,
            @RequestParam Long user2Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(chatService.getChatHistory(user1Id, user2Id, page, size));
    }

    // ── REST: Đánh dấu đã đọc ───────────────────────────────────────────────

    @PutMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            Authentication auth) {
        chatService.markAsRead(senderId, receiverId);
        return ResponseEntity.noContent().build();
    }

    // ── REST: Đếm chưa đọc ──────────────────────────────────────────────────

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @RequestParam Long senderId,
            @RequestParam Long receiverId) {
        return ResponseEntity.ok(Map.of("unreadCount", chatService.countUnread(senderId, receiverId)));
    }

    // ── REST: Xóa conversation (soft-delete) ────────────────────────────────

    /**
     * DELETE /api/chat/conversations/{otherId}
     * Xóa conversation từ phía người dùng hiện tại.
     * Conversation biến mất khỏi list; nhắn lại thì xuất hiện conversation mới (không có tin cũ).
     */
    @DeleteMapping("/conversations/{otherId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long otherId) {
        chatService.deleteConversation(otherId);
        return ResponseEntity.noContent().build();
    }

    // ── REST: Kiểm tra trạng thái block ─────────────────────────────────────

    /**
     * GET /api/chat/block-status/{otherUserId}
     * Cho client biết: tôi có bị block không, tôi có đang block không.
     */
    @GetMapping("/block-status/{otherUserId}")
    public ResponseEntity<Map<String, Object>> getBlockStatus(@PathVariable Long otherUserId) {
        return ResponseEntity.ok(chatService.getBlockStatus(otherUserId));
    }

    // ── REST: Upload ảnh / voice ─────────────────────────────────────────────

    /**
     * POST /api/chat/upload?type=IMAGE hoặc type=VOICE
     * Nhận multipart file, upload lên Cloudinary, trả về { mediaUrl, type }.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        return ResponseEntity.ok(chatService.uploadMedia(file, type));
    }
}
