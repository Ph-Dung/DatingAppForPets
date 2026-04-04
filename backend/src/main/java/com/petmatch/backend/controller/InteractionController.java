package com.petmatch.backend.controller;

import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.dto.request.ReportRequest;
import com.petmatch.backend.entity.Block;
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

    /** Đánh giá người dùng sau khi match */
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

    /** Chặn người dùng – dùng JWT để xác định blocker */
    @PostMapping("/blocks/{targetUserId}")
    public ResponseEntity<Block> blockUser(@PathVariable Long targetUserId) {
        return ResponseEntity.ok(interactionService.blockUser(targetUserId));
    }

    /** Bỏ chặn người dùng theo targetUserId */
    @DeleteMapping("/blocks/{targetUserId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long targetUserId) {
        interactionService.unblockUser(targetUserId);
        return ResponseEntity.noContent().build();
    }

    /** Danh sách người mình đã chặn */
    @GetMapping("/blocks")
    public ResponseEntity<List<Block>> getMyBlocks() {
        return ResponseEntity.ok(interactionService.getMyBlocks());
    }

    // ── REPORTS ──────────────────────────────────────────

    /** Báo cáo vi phạm (pet profile, user, ...) */
    @PostMapping("/reports")
    public ResponseEntity<Report> submitReport(@Valid @RequestBody ReportRequest req) {
        return ResponseEntity.status(201).body(interactionService.submitReport(req));
    }
}
