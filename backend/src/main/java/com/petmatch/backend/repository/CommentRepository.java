package com.petmatch.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petmatch.backend.entity.Comment;
import com.petmatch.backend.entity.Post;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    /**
     * Repository cho `Comment`.
     * - Hỗ trợ lấy comment theo post, lấy comment top-level (parentComment is null),
     *   và lấy replies theo parent comment.
     */
    List<Comment> findAllByPostOrderByCreatedAtAsc(Post post);
    List<Comment> findAllByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post);
    List<Comment> findAllByParentCommentOrderByCreatedAtAsc(Comment parentComment);

    // Xóa tất cả comment theo user (dùng khi xoá user)
    long deleteByUserId(Long userId);
}
