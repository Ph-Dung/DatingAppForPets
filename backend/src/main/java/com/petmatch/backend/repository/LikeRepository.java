package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);
    long countByPost(Post post);
    boolean existsByUserAndPost(User user, Post post);
}
