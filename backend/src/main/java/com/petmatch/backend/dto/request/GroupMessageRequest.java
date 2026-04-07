package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request gửi tin nhắn vào nhóm qua REST hoặc WebSocket.
 */
@Data
public class GroupMessageRequest {

    @NotBlank
    private String content;
}
