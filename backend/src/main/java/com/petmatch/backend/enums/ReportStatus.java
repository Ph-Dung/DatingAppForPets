package com.petmatch.backend.enums;

/**
 * Trạng thái báo cáo trong quy trình kiểm duyệt.
 * - `PENDING`: chờ admin xử lý
 * - `RESOLVED`: đã xử lý và hành động đã thực hiện
 * - `DISMISSED`: bị bác bỏ/không chấp nhận
 */
public enum ReportStatus { PENDING, RESOLVED, DISMISSED }
