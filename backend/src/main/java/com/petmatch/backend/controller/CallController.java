package com.petmatch.backend.controller;

import com.petmatch.backend.dto.CallRequest;
import com.petmatch.backend.dto.SignalingMessage;
import com.petmatch.backend.entity.CallHistory;
import com.petmatch.backend.entity.CallStatus;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * Fix #3: callerId lấy từ JWT (Authentication), không phải @RequestParam.
     * Fix #15: Sau khi tạo record, gửi INCOMING_CALL signal tới callee qua WebSocket.
     */
    @PostMapping("/start")
    public ResponseEntity<CallHistory> startCall(
            Authentication auth,
            @RequestBody CallRequest request) {

        // Lấy callerId từ JWT
        Long callerId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        CallHistory callHistory = callService.initiateCall(callerId, request);

        // Fix #15: Thông báo INCOMING_CALL tới callee qua WebSocket signal
        SignalingMessage incomingSignal = SignalingMessage.builder()
                .senderId(callerId)
                .receiverId(request.getCalleeId())
                .type("INCOMING_CALL")
                .data("{\"callId\":" + callHistory.getId() +
                      ",\"callType\":\"" + request.getType().name() + "\"}")
                .build();

        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getCalleeId()),
                "/queue/signals",
                incomingSignal
        );

        return ResponseEntity.ok(callHistory);
    }

    @PutMapping("/{callId}/end")
    public ResponseEntity<CallHistory> endCall(
            @PathVariable Long callId,
            @RequestParam CallStatus status) {
        return ResponseEntity.ok(callService.endCall(callId, status));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<CallHistory>> getCallHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(callService.getUserCallHistory(userId));
    }
}

