package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.MatchRequestDto;
import com.petmatch.backend.dto.request.DislikeRequestDto;
import com.petmatch.backend.dto.response.MatchRequestResponse;
import com.petmatch.backend.dto.response.SuperLikeStatusResponse;
import com.petmatch.backend.enums.MatchStatus;
import com.petmatch.backend.service.MatchRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller: Pet-level matching (Like/Super Like/Dislike)
 * 
 * Endpoints:
 * - POST /api/matches: send like/super-like (auto-match if mutual)
 * - POST /api/matches/dislike: record skip/dislike
 * - GET /api/matches/super-like-status: check quota (1/ngày)
 * - GET /api/matches/sent, /received, /matched: queries
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchRequestController {
    private final MatchRequestService matchService;

    // ════════════════════════════════════════════════════════════════
    // SEND MATCH REQUEST
    // ════════════════════════════════════════════════════════════════
    
    /** 
     * POST /api/matches: Gửi like/super-like
     * 
     * Auto-match: nếu mutual → ACCEPTED + create Match
     * Super-like quota: 1/ngày/pet (reset 00:00)
     */
    @PostMapping
    public ResponseEntity<MatchRequestResponse> send(
            @Valid @RequestBody MatchRequestDto req) {
        boolean isSuperLike = Boolean.TRUE.equals(req.getIsSuperLike());
        return ResponseEntity.status(201)
                .body(matchService.sendRequest(req.getReceiverPetId(), isSuperLike));
    }

    // ════════════════════════════════════════════════════════════════
    // DISLIKE (SKIP)
    // ════════════════════════════════════════════════════════════════
    
    /** POST /api/matches/dislike: Ghi nhận bỏ qua (skip) */
    @PostMapping("/dislike")
    public ResponseEntity<Void> recordDislike(
            @Valid @RequestBody DislikeRequestDto req) {
        matchService.recordDislike(req.getDislikedPetId());
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════
    // SUPER LIKE STATUS
    // ════════════════════════════════════════════════════════════════
    
    /** 
     * GET /api/matches/super-like-status: Kiểm tra quota
     * 
     * Response: canSuperLike, nextResetAt (00:00 ngày mai), usedToday (0|1)
     */
    @GetMapping("/super-like-status")
    public ResponseEntity<SuperLikeStatusResponse> superLikeStatus() {
        return ResponseEntity.ok(matchService.getSuperLikeStatus());
    }

    // ════════════════════════════════════════════════════════════════
    // MANUAL RESPOND
    // ════════════════════════════════════════════════════════════════
    
    /** 
     * PATCH /api/matches/{matchId}/respond: Manual accept/reject
     * 
     * (Thường không dùng vì auto-match; giữ cho flexibility)
     */
    @PatchMapping("/{matchId}/respond")
    public ResponseEntity<MatchRequestResponse> respond(
            @PathVariable Long matchId,
            @RequestParam MatchStatus status) {
        return ResponseEntity.ok(matchService.respond(matchId, status));
    }

    // ════════════════════════════════════════════════════════════════
    // QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** GET /api/matches/sent: Danh sách like mình đã gửi */
    @GetMapping("/sent")
    public ResponseEntity<List<MatchRequestResponse>> sent() {
        return ResponseEntity.ok(matchService.getMySentRequests());
    }

    /** GET /api/matches/received: Ai đã like mình (super-like first) */
    @GetMapping("/received")
    public ResponseEntity<List<MatchRequestResponse>> whoLikedMe() {
        return ResponseEntity.ok(matchService.getWhoLikedMe());
    }

    /** GET /api/matches/matched: Danh sách match thành công (mutual + chat ready) */
    @GetMapping("/matched")
    public ResponseEntity<List<MatchRequestResponse>> matched() {
        return ResponseEntity.ok(matchService.getMyMatches());
    }
}