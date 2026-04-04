package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SuperLikeStatusResponse {
    /** true nếu hôm nay chưa dùng super like */
    private boolean canSuperLike;
    /** Thời điểm reset (00:00 ngày hôm sau) */
    private LocalDateTime nextResetAt;
    /** Số super like đã dùng hôm nay (0 hoặc 1) */
    private int usedToday;
}
