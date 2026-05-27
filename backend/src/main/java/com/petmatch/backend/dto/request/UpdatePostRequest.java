package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO: Yêu cầu cập nhật một bài đăng.
 * - `content` là bắt buộc (hiện bộ UI/flow yêu cầu không để trống khi cập nhật).
 * - Các trường khác optional để cập nhật từng phần.
 */
@Data
public class UpdatePostRequest {
    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content; // Nội dung mới của bài viết

    private String imageUrl; // Chuỗi URL ảnh (có thể là danh sách đã ghép)
    private String location; // Vị trí cập nhật
}
