package com.petmatch.backend.controller;

import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.Review;
import com.petmatch.backend.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // --- REVIEWS ---

    @PostMapping("/reviews")
    public ResponseEntity<Review> createReview(
            @RequestParam Long reviewerId, // In a real app, extract from JWT
            @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(interactionService.createReview(reviewerId, request));
    }

    @GetMapping("/reviews/user/{userId}")
    public ResponseEntity<List<Review>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(interactionService.getUserReviews(userId));
    }

    // --- BLOCKS ---

    @PostMapping("/blocks")
    public ResponseEntity<Block> blockUser(
            @RequestParam Long blockerId, // Extract from JWT ideally
            @RequestParam Long blockedId) {
        return ResponseEntity.ok(interactionService.blockUser(blockerId, blockedId));
    }

    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long blockId) {
        interactionService.unblockUser(blockId);
        return ResponseEntity.noContent().build();
    }
}
