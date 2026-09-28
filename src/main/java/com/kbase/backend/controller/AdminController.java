package com.kbase.backend.controller;

import com.kbase.backend.dto.UserResponse;
import com.kbase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Các endpoint quản trị — chỉ ROLE_ADMIN mới truy cập được.
 * Được bảo vệ tại 2 lớp: SecurityConfig (URL-level) + @PreAuthorize (method-level).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin Controller", description = "Quản lý người dùng (chỉ Admin)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/admin/users — Lấy danh sách toàn bộ user trong hệ thống.
     */
    @GetMapping("/users")
    @Operation(summary = "Lấy danh sách tất cả người dùng (Admin only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * DELETE /api/admin/users/{id} — Xóa user khỏi hệ thống.
     */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Xóa người dùng theo ID (Admin only)")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Xóa người dùng thành công!"));
    }
}
