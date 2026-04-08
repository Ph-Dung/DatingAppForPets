package com.petmatch.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.petmatch.backend.entity.HiddenPost;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;

@Repository
public interface HiddenPostRepository extends JpaRepository<HiddenPost, Long> {

    Optional<HiddenPost> findByUserAndPost(User user, Post post);

    @Query("select hp.post.id from HiddenPost hp where hp.user.id = :userId")
    List<Long> findPostIdsByUserId(Long userId);
}
