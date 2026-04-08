package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findAllByUserOrderByCreatedAtDesc(User user);
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
