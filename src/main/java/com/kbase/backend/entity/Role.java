package com.kbase.backend.entity;

/**
 * Enum định nghĩa 3 vai trò trong hệ thống KBase.
 * - ROLE_ADMIN: Quản trị viên hệ thống, toàn quyền quản lý User.
 * - ROLE_OWNER: Chủ dự án, có thể tạo Project và mời thành viên.
 * - ROLE_USER:  Người dùng thông thường, được tham gia Project và upload tài liệu.
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_OWNER,
    ROLE_USER
}

