package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.MatchRequestDto;
import com.petmatch.backend.dto.response.MatchRequestResponse;
import com.petmatch.backend.enums.MatchStatus;
import com.petmatch.backend.service.MatchRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches") @RequiredArgsConstructor
public class MatchRequestController {
    private final MatchRequestService matchService;

    @PostMapping
    public ResponseEntity<MatchRequestResponse> send(
            @Valid @RequestBody MatchRequestDto req) {
        return ResponseEntity.status(201)
                .body(matchService.sendRequest(req.getReceiverPetId()));
    }

    // ACCEPTED hoặc REJECTED
    @PatchMapping("/{matchId}/respond")
    public ResponseEntity<MatchRequestResponse> respond(
            @PathVariable UUID matchId,
            @RequestParam MatchStatus status) {
        return ResponseEntity.ok(matchService.respond(matchId, status));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<MatchRequestResponse>> sent() {
        return ResponseEntity.ok(matchService.getMySentRequests());
    }

    @GetMapping("/received")
    public ResponseEntity<List<MatchRequestResponse>> received() {
        return ResponseEntity.ok(matchService.getMyPendingReceived());
    }

    // Danh sách match thành công → hiển thị nút "Chat" nếu canOpenConversation = true
    @GetMapping("/matched")
    public ResponseEntity<List<MatchRequestResponse>> matched() {
        return ResponseEntity.ok(matchService.getMyMatches());
    }
}