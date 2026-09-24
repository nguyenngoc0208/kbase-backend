package com.kbase.backend.dto;

import com.kbase.backend.entity.Folder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FolderResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;

    public static FolderResponse fromEntity(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .createdAt(folder.getCreatedAt())
                .build();
    }
}