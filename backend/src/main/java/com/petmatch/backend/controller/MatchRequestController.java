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

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchRequestController {
    private final MatchRequestService matchService;

    /** Gửi like / super like */
    @PostMapping
    public ResponseEntity<MatchRequestResponse> send(
            @Valid @RequestBody MatchRequestDto req) {
        boolean isSuperLike = Boolean.TRUE.equals(req.getIsSuperLike());
        return ResponseEntity.status(201)
                .body(matchService.sendRequest(req.getReceiverPetId(), isSuperLike));
    }

    /** Ghi nhận bỏ qua (dislike) */
    @PostMapping("/dislike")
    public ResponseEntity<Void> recordDislike(
            @Valid @RequestBody DislikeRequestDto req) {
        matchService.recordDislike(req.getDislikedPetId());
        return ResponseEntity.noContent().build();
    }

    /** Kiểm tra quota Super Like hôm nay */
    @GetMapping("/super-like-status")
    public ResponseEntity<SuperLikeStatusResponse> superLikeStatus() {
        return ResponseEntity.ok(matchService.getSuperLikeStatus());
    }

    /** Respond ACCEPTED / REJECTED (dùng cho manual mode nếu cần) */
    @PatchMapping("/{matchId}/respond")
    public ResponseEntity<MatchRequestResponse> respond(
            @PathVariable Long matchId,
            @RequestParam MatchStatus status) {
        return ResponseEntity.ok(matchService.respond(matchId, status));
    }

    /** Danh sách match request mình đã gửi */
    @GetMapping("/sent")
    public ResponseEntity<List<MatchRequestResponse>> sent() {
        return ResponseEntity.ok(matchService.getMySentRequests());
    }

    /** Ai đã like / super-like mình – super like xếp đầu */
    @GetMapping("/received")
    public ResponseEntity<List<MatchRequestResponse>> whoLikedMe() {
        return ResponseEntity.ok(matchService.getWhoLikedMe());
    }

    /** Danh sách match thành công (mutual) */
    @GetMapping("/matched")
    public ResponseEntity<List<MatchRequestResponse>> matched() {
        return ResponseEntity.ok(matchService.getMyMatches());
    }
}