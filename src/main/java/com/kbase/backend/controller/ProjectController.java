package com.kbase.backend.controller;

import com.kbase.backend.dto.InviteMemberRequest;
import com.kbase.backend.dto.ProjectMemberResponse;
import com.kbase.backend.dto.ProjectRequest;
import com.kbase.backend.dto.ProjectResponse;
import com.kbase.backend.entity.User;
import com.kbase.backend.service.ProjectService;
import com.kbase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Controller", description = "Quản lý dự án và thành viên")
@Transactional(readOnly = true)
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * POST /api/projects — Tạo dự án mới (chỉ ROLE_OWNER).
     * SecurityConfig đã chặn ROLE_USER ở tầng filter; service kiểm tra lần 2.
     */
    @PostMapping
    @Transactional
    @Operation(summary = "Tạo dự án mới (chỉ Owner)")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {

        User owner = userService.getCurrentUser(authentication.getName());
        ProjectResponse response = projectService.createProject(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/projects — Danh sách project của user hiện tại (owner + member).
     */
    @GetMapping
    @Operation(summary = "Danh sách dự án của tôi (owner hoặc member)")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(Authentication authentication) {
        User user = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(projectService.getProjectsForUser(user));
    }

    /**
     * POST /api/projects/{projectId}/members — Owner mời thành viên qua email.
     */
    @PostMapping("/{projectId}/members")
    @Transactional
    @Operation(summary = "Mời thành viên vào dự án (chỉ Owner)")
    public ResponseEntity<ProjectMemberResponse> inviteMember(
            @PathVariable Long projectId,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {

        User requester = userService.getCurrentUser(authentication.getName());
        ProjectMemberResponse response = projectService.inviteMember(projectId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/projects/{projectId}/members — Xem danh sách thành viên.
     */
    @GetMapping("/{projectId}/members")
    @Operation(summary = "Danh sách thành viên của dự án")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(
            @PathVariable Long projectId,
            Authentication authentication) {

        User requester = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(projectService.getMembers(projectId, requester));
    }

    /**
     * DELETE /api/projects/{projectId} — Chỉ Owner mới được xóa dự án.
     */
    @DeleteMapping("/{projectId}")
    @Transactional
    @Operation(summary = "Xóa dự án (chỉ Owner)")
    public ResponseEntity<Map<String, String>> deleteProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        User requester = userService.getCurrentUser(authentication.getName());
        projectService.deleteProject(projectId, requester);
        return ResponseEntity.ok(Map.of("message", "Xóa dự án thành công!"));
    }
}
