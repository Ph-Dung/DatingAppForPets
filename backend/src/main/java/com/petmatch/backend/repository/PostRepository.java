package com.petmatch.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findAllByUserOrderByCreatedAtDesc(User user);
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdIn(List<Long> userIds);
    long deleteByUserId(Long userId);
}
