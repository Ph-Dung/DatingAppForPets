package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    /**
     * Repository cho `Like`.
     * - Tìm like bởi user và post, đếm like cho 1 post, kiểm tra tồn tại.
     */
    Optional<Like> findByUserAndPost(User user, Post post);
    long countByPost(Post post);
    boolean existsByUserAndPost(User user, Post post);
}
