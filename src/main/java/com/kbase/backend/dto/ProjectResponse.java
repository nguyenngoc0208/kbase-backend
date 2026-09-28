package com.kbase.backend.dto;

import com.kbase.backend.entity.Project;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;
    private LocalDateTime createdAt;

    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner().getId())
                .ownerEmail(project.getOwner().getEmail())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
