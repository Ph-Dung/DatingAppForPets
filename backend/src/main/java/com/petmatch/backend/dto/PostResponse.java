package com.petmatch.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private String location;
    private String ownerName;
    private String ownerAvatar;
    private Long ownerId;
    private LocalDateTime createdAt;
    private long likesCount;
    private long commentsCount;
    private boolean isLiked;
}
