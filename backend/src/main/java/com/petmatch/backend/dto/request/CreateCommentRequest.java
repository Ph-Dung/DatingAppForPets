package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO: Yêu cầu tạo bình luận cho một bài viết hoặc reply.
 * - `content` bắt buộc.
 */
@Data
public class CreateCommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content; // Nội dung bình luận
}
