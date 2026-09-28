package com.kbase.backend.repository;

import com.kbase.backend.entity.Project;
import com.kbase.backend.entity.ProjectMember;
import com.kbase.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** Kiểm tra user có phải là member của project không */
    boolean existsByProjectAndUser(Project project, User user);

    /** Tìm bản ghi member theo project và user */
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    /** Lấy toàn bộ thành viên của một project */
    List<ProjectMember> findByProjectOrderByJoinedAtAsc(Project project);

    /** Xóa toàn bộ member khi project bị xóa */
    void deleteAllByProject(Project project);
}
