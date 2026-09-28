package com.kbase.backend.repository;

import com.kbase.backend.entity.Project;
import com.kbase.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Tìm tất cả project mà user là owner */
    List<Project> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Tìm tất cả project mà user là owner HOẶC là member.
     * Dùng DISTINCT để tránh duplicate khi user vừa là owner vừa có trong project_members.
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectMember pm ON pm.project = p " +
           "WHERE p.owner = :user OR pm.user = :user " +
           "ORDER BY p.createdAt DESC")
    List<Project> findAllByOwnerOrMember(@Param("user") User user);
}
