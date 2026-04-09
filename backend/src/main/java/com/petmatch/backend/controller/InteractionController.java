package com.petmatch.backend.controller;

import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.dto.request.ReportRequest;
import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.BlockLevel;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.Review;
import com.petmatch.backend.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // ── REVIEWS ──────────────────────────────────────────

    @PostMapping("/reviews/{revieweeId}")
    public ResponseEntity<Review> createReview(
            @PathVariable Long revieweeId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(interactionService.createReview(revieweeId, request));
    }

    @GetMapping("/reviews/user/{userId}")
    public ResponseEntity<List<Review>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(interactionService.getUserReviews(userId));
    }

    // ── BLOCKS ───────────────────────────────────────────

    /**
     * Chặn người dùng với cấp độ chặn.
     * level: MESSAGE | CALL | ALL (mặc định ALL)
     */
    @PostMapping("/blocks/{targetUserId}")
    public ResponseEntity<Block> blockUser(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "ALL") BlockLevel level) {
        return ResponseEntity.ok(interactionService.blockUser(targetUserId, level));
    }

    @DeleteMapping("/blocks/{targetUserId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long targetUserId) {
        interactionService.unblockUser(targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<com.petmatch.backend.dto.response.BlockResponse>> getMyBlocks() {
        return ResponseEntity.ok(interactionService.getMyBlocks());
    }

    // ── REPORTS ──────────────────────────────────────────

    @PostMapping("/reports")
    public ResponseEntity<Report> submitReport(@Valid @RequestBody ReportRequest req) {
        return ResponseEntity.status(201).body(interactionService.submitReport(req));
    }
}
