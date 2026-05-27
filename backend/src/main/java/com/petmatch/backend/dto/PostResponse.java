package com.petmatch.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO: Dữ liệu trả về cho một bài đăng trong API.
 * Bao gồm thông tin nội dung, tác giả, số lượng tương tác và trạng thái like của user hiện tại.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id; // ID bài viết
    private String content; // Nội dung bài viết
    private String imageUrl; // Chuỗi URL ảnh (có thể là nhiều URL ghép bằng dấu ,)
    private String location; // Vị trí bài viết
    private String ownerName; // Tên chủ bài
    private String ownerAvatar; // URL avatar chủ bài (user hoặc pet fallback)
    private Long ownerId; // ID chủ bài
    private LocalDateTime createdAt; // Thời gian tạo
    private long likesCount; // Số lượt like
    private long commentsCount; // Số lượt comment
    private boolean isLiked; // Có phải user hiện tại đã like chưa
}
