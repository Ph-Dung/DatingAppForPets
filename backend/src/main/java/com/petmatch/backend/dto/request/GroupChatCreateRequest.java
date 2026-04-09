package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request để tạo nhóm chat mới.
 * Người tạo lấy từ JWT, không cần truyền lên.
 */
@Data
public class GroupChatCreateRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    private String avatarUrl;

    /** Danh sách userId của các thành viên ban đầu (ngoài người tạo) */
    @NotNull
    @Size(min = 1, message = "Nhóm phải có ít nhất 1 thành viên khác ngoài người tạo")
    private List<Long> memberIds;
}
