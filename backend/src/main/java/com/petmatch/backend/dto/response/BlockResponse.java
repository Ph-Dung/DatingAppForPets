package com.petmatch.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockResponse {
    private Long id;
    private Long blockedUserId;
    private String blockedUserName;
    private String blockedUserAvatarUrl;
    private LocalDateTime createdAt;
}
