package com.petmatch.backend.entity;

public enum BlockLevel {
    /** Chỉ chặn tin nhắn */
    MESSAGE,
    /** Chỉ chặn cuộc gọi */
    CALL,
    /** Chặn tất cả: tin nhắn, cuộc gọi và ẩn khỏi tìm kiếm */
    ALL
}
