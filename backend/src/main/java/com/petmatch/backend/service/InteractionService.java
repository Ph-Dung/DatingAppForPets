package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.ReportRequest;
import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.entity.*;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final ReviewRepository reviewRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final MessageRepository messageRepository;
    private final com.petmatch.backend.repository.PetProfileRepository petProfileRepo;
    private final com.petmatch.backend.repository.PetPhotoRepository petPhotoRepo;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", NOT_FOUND));
    }

    // ── REVIEWS ──────────────────────────────────────────
    @Transactional
    public Review createReview(Long revieweeId, ReviewRequest request) {
        User reviewer = currentUser();
        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại", NOT_FOUND));

        if (reviewer.getId().equals(revieweeId)) {
            throw new AppException("Không thể tự đánh giá bản thân", BAD_REQUEST);
        }

        // Kiểm tra 2 người có match không
        // boolean hasMatch = matchRepository.findMatchByUserIds(reviewer.getId(), revieweeId).isPresent();
        // if (!hasMatch) {
        //     throw new AppException("Chỉ có thể đánh giá người mà bạn đã match", FORBIDDEN);
        // }

        // Kiểm tra đã từng nhắn tin chưa (phải nói chuyện trước khi review)
        if (!messageRepository.existsChat(reviewer, reviewee)) {
            throw new AppException("Bạn cần nhắn tin với người này trước khi đánh giá", FORBIDDEN);
        }

        // Ngăn review trùng lặp
        if (reviewRepository.existsByReviewerAndReviewee(reviewer, reviewee)) {
            throw new AppException("Bạn đã đánh giá người này rồi", CONFLICT);
        }

        if (request.getRating() < 1 || request.getRating() > 5)
            throw new AppException("Điểm đánh giá phải từ 1 đến 5", BAD_REQUEST);

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
                .orElseThrow(() -> new AppException("Người dùng không tồn tại", NOT_FOUND));
        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
    }

    // ── BLOCKS ───────────────────────────────────────────
    @Transactional
    public Block blockUser(Long targetUserId, com.petmatch.backend.entity.BlockLevel level) {
        User blocker = currentUser();
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại", NOT_FOUND));

        if (blocker.getId().equals(targetUserId))
            throw new AppException("Không thể tự chặn chính mình", BAD_REQUEST);

        com.petmatch.backend.entity.BlockLevel effectiveLevel =
                (level != null) ? level : com.petmatch.backend.entity.BlockLevel.ALL;

        // Nếu đã chặn thì cập nhật level mới
        blockRepository.findByBlockerAndBlocked(blocker, blocked).ifPresent(existing -> {
            existing.setLevel(effectiveLevel);
            blockRepository.save(existing);
        });

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return blockRepository.findByBlockerAndBlocked(blocker, blocked).get();
        }

        return blockRepository.save(Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .level(effectiveLevel)
                .build());
    }

    @Transactional
    public void unblockUser(Long targetUserId) {
        User blocker = currentUser();
        blockRepository.findByBlockerIdAndBlockedId(blocker.getId(), targetUserId)
                .ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<com.petmatch.backend.dto.response.BlockResponse> getMyBlocks() {
        return blockRepository.findByBlocker(currentUser()).stream()
                .map(b -> com.petmatch.backend.dto.response.BlockResponse.builder()
                        .id(b.getId())
                        .blockedUserId(b.getBlocked().getId())
                        .blockedUserName(b.getBlocked().getFullName())
                        .blockedUserAvatarUrl(getAvatarForUser(b.getBlocked().getId()))
                        .createdAt(b.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private String getAvatarForUser(Long userId) {
        // Find avatar via PetPhoto
        return userRepository.findById(userId)
                .flatMap(u -> petProfileRepo.findByOwnerId(u.getId()))
                .flatMap(p -> petPhotoRepo.findByPetIdAndIsAvatarTrue(p.getId()))
                .map(com.petmatch.backend.entity.PetPhoto::getPhotoUrl)
                .orElse(null);
    }

    // ── REPORTS ──────────────────────────────────────────
    @Transactional
    public Report submitReport(ReportRequest req) {
        User reporter = currentUser();
        ReportTargetType targetType;
        try {
            targetType = ReportTargetType.valueOf(req.getTargetType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Loại báo cáo không hợp lệ: " + req.getTargetType(), BAD_REQUEST);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(req.getTargetId())
                .reason(req.getReason())
                .status(ReportStatus.PENDING)
                .build();

        return reportRepository.save(report);
    }
}
