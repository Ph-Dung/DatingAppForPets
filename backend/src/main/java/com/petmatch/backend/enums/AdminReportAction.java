package com.petmatch.backend.enums;

/**
 * Hành động mà admin có thể thực thi khi xử lý 1 báo cáo.
 * Có thể map thành cảnh báo, xóa nội dung tự động, khoá user, hoặc dismiss.
 */
public enum AdminReportAction {
    WARN_USER,
    AUTO_DELETE_PHOTO,
    AUTO_DELETE_PET,
    BAN_USER,
    DISMISS
}
