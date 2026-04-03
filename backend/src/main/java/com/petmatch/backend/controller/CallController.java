package com.petmatch.backend.controller;

import com.petmatch.backend.dto.CallRequest;
import com.petmatch.backend.entity.CallHistory;
import com.petmatch.backend.entity.CallStatus;
import com.petmatch.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @PostMapping("/start")
    public ResponseEntity<CallHistory> startCall(
            @RequestParam Long callerId, // Ideally get from JWT
            @RequestBody CallRequest request) {
        return ResponseEntity.ok(callService.initiateCall(callerId, request));
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
