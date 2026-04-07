package com.petmatch.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private Long createdById;
    private LocalDateTime createdAt;
    private List<GroupMemberResponse> members;
    private String lastMessage;   // Nội dung tin nhắn cuối cùng (null nếu chưa có)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberResponse {
        private Long userId;
        private String fullName;
        private String avatarUrl;
        private String role;
        private LocalDateTime joinedAt;
    }
}

