package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.ReportRequest;
import com.petmatch.backend.dto.ReviewRequest;
import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.Review;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.BlockRepository;
import com.petmatch.backend.repository.ReportRepository;
import com.petmatch.backend.repository.ReviewRepository;
import com.petmatch.backend.repository.UserRepository;
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
    public Block blockUser(Long targetUserId) {
        User blocker = currentUser();
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại", NOT_FOUND));

        if (blocker.getId().equals(targetUserId))
            throw new AppException("Không thể tự chặn chính mình", BAD_REQUEST);

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked))
            throw new AppException("Đã chặn người này rồi", CONFLICT);

        return blockRepository.save(Block.builder().blocker(blocker).blocked(blocked).build());
    }

    @Transactional
    public void unblockUser(Long targetUserId) {
        User blocker = currentUser();
        blockRepository.findByBlockerIdAndBlockedId(blocker.getId(), targetUserId)
                .ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Block> getMyBlocks() {
        return blockRepository.findByBlocker(currentUser());
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
