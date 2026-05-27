package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO: Yêu cầu cập nhật nội dung bình luận.
 */
@Data
public class UpdateCommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content; // Nội dung mới của bình luận
}
