package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO: Yêu cầu tạo bài đăng mới từ frontend.
 * - `content` là bắt buộc.
 * - `imageUrl` có thể truyền từ frontend khi upload độc lập hoặc null.
 * - `location` là tuỳ chọn mô tả vị trí.
 */
@Data
public class CreatePostRequest {
    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content; // Nội dung chính của bài viết

    private String imageUrl; // URL ảnh (nếu frontend đã upload trước đó)
    private String location; // Vị trí / địa điểm kèm theo bài viết
}
