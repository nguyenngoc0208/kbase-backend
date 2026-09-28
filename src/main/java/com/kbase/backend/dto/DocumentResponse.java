package com.kbase.backend.dto;

import com.kbase.backend.entity.Document;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {
    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private Long projectId;
    private String uploadedByEmail;
    private LocalDateTime createdAt;

    public static DocumentResponse fromEntity(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFileName(doc.getOriginalFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .projectId(doc.getProject() != null ? doc.getProject().getId() : null)
                .uploadedByEmail(doc.getUploadedBy() != null ? doc.getUploadedBy().getEmail() : null)
                .createdAt(doc.getCreatedAt())
                .build();
    }
}