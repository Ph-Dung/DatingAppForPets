package com.petmatch.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String userName;
    private String userAvatar;
    private LocalDateTime createdAt;
    private Long postId;
    private Long parentCommentId;
    private List<CommentResponse> replies;
}
