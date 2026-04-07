package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private String imageUrl;
    private String location;
}
