package com.petmatch.backend.service;

import com.petmatch.backend.config.ResourceNotFoundException;
import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.Review;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.repository.BlockRepository;
import com.petmatch.backend.repository.ReviewRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final ReviewRepository reviewRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review createReview(Long reviewerId, ReviewRequest request) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getRevieweeId()));

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> getUserReviews(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Block blockUser(Long blockerId, Long blockedId) {
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", blockerId));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", blockedId));

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new IllegalStateException("User is already blocked");
        }

        Block block = Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        return blockRepository.save(block);
    }
    
    @Transactional
    public void unblockUser(Long blockId) {
        blockRepository.deleteById(blockId);
    }
}
