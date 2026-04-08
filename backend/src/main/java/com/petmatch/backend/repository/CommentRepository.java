package com.petmatch.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmatch.backend.entity.Comment;
import com.petmatch.backend.entity.Post;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostOrderByCreatedAtAsc(Post post);
    List<Comment> findAllByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post);
    List<Comment> findAllByParentCommentOrderByCreatedAtAsc(Comment parentComment);
    long deleteByUserId(Long userId);
}
