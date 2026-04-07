package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Review;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee);

    /** Kiểm tra reviewer đã review reviewee này chưa — tránh review trùng lặp */
    boolean existsByReviewerAndReviewee(User reviewer, User reviewee);
}

