package com.kbase.backend.dto;

import com.kbase.backend.entity.ProjectMember;
import com.kbase.backend.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectMemberResponse {
    private Long userId;
    private String email;
    private Role role;
    private LocalDateTime joinedAt;

    public static ProjectMemberResponse fromEntity(ProjectMember member) {
        return ProjectMemberResponse.builder()
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .role(member.getUser().getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
