package com.kbase.backend.service;

import com.kbase.backend.dto.InviteMemberRequest;
import com.kbase.backend.dto.ProjectMemberResponse;
import com.kbase.backend.dto.ProjectRequest;
import com.kbase.backend.dto.ProjectResponse;
import com.kbase.backend.entity.Project;
import com.kbase.backend.entity.ProjectMember;
import com.kbase.backend.entity.Role;
import com.kbase.backend.entity.User;
import com.kbase.backend.exception.AccessDeniedException;
import com.kbase.backend.exception.DuplicateResourceException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.repository.DocumentRepository;
import com.kbase.backend.repository.ProjectMemberRepository;
import com.kbase.backend.repository.ProjectRepository;
import com.kbase.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Service xử lý logic quản lý dự án và thành viên dự án
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          DocumentRepository documentRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    // Tạo dự án mới (cho phép ROLE_OWNER hoặc ROLE_ADMIN)
    public ProjectResponse createProject(ProjectRequest request, User owner) {
        if (owner.getRole() != Role.ROLE_OWNER && owner.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Chỉ người dùng có vai trò OWNER hoặc ADMIN mới có thể tạo dự án.");
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    // Lấy danh sách dự án (Đồng nhất hệ thống cho cả Admin, Owner, Member)
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForUser(User user) {
        List<Project> projectEntities = projectRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ProjectResponse> responseList = new ArrayList<>();

        for (Project project : projectEntities) {
            responseList.add(ProjectResponse.fromEntity(project));
        }

        return responseList;
    }

    // Mời thành viên mới vào dự án qua email (dành cho Owner và Admin)
    public ProjectMemberResponse inviteMember(Long projectId, InviteMemberRequest request, User requester) {
        Project project = getProjectOrThrow(projectId);
        assertIsOwner(project, requester);

        User invitee = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + request.getEmail()));

        if (invitee.getId().equals(requester.getId())) {
            throw new DuplicateResourceException("Bạn đã là Owner của dự án này, không cần tự mời.");
        }

        if (memberRepository.existsByProjectAndUser(project, invitee)) {
            throw new DuplicateResourceException("Người dùng " + invitee.getEmail() + " đã là thành viên dự án.");
        }

        ProjectMember newMember = ProjectMember.builder()
                .project(project)
                .user(invitee)
                .build();

        return ProjectMemberResponse.fromEntity(memberRepository.save(newMember));
    }

    // Lấy danh sách thành viên của dự án
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long projectId, User requester) {
        Project project = getProjectOrThrow(projectId);
        assertIsMemberOrOwner(project, requester);

        List<ProjectMember> memberList = memberRepository.findByProjectOrderByJoinedAtAsc(project);
        List<ProjectMemberResponse> responseList = new ArrayList<>();

        for (ProjectMember member : memberList) {
            responseList.add(ProjectMemberResponse.fromEntity(member));
        }

        return responseList;
    }

    // Xóa dự án và toàn bộ dữ liệu thành viên, tài liệu thuộc dự án (chỉ Owner và Admin)
    public void deleteProject(Long projectId, User requester) {
        Project project = getProjectOrThrow(projectId);
        assertIsOwner(project, requester);

        memberRepository.deleteAllByProject(project);
        documentRepository.deleteAllByProject(project);
        projectRepository.delete(project);
    }

    // Tìm dự án theo ID hoặc ném ngoại lệ 404
    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án với ID: " + projectId));
    }

    // Kiểm tra người dùng có quyền truy cập/xem dự án không (Đồng nhất hệ thống)
    public boolean isOwnerOrMember(Project project, User user) {
        return true;
    }

    // Bắt buộc người dùng phải là Owner hoặc Admin của dự án
    private void assertIsOwner(Project project, User user) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Chỉ Trưởng dự án (Owner) hoặc Admin mới có quyền thực hiện thao tác này.");
        }
    }

    // Bắt buộc người dùng phải có quyền xem dự án
    private void assertIsMemberOrOwner(Project project, User user) {
        if (!isOwnerOrMember(project, user)) {
            throw new AccessDeniedException("Bạn không có quyền xem dự án này.");
        }
    }
}