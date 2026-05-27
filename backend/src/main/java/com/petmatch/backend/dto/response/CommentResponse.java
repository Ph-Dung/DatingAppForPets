package com.petmatch.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO: Dữ liệu trả về cho một bình luận.
 * - `replies` chứa danh sách các phản hồi (reply) theo cấu trúc đệ quy.
 */
@Data
@Builder
public class CommentResponse {
    private Long id; // ID của bình luận
    private String content; // Nội dung bình luận
    private Long userId; // ID tác giả
    private String userName; // Tên tác giả
    private String userAvatar; // Avatar tác giả
    private LocalDateTime createdAt; // Thời gian tạo
    private Long postId; // ID bài viết thuộc về
    private Long parentCommentId; // Nếu là reply, parentCommentId != null
    private List<CommentResponse> replies; // Danh sách reply (nếu có)
}
