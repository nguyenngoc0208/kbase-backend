package com.kbase.backend.controller;

import com.kbase.backend.dto.DocumentResponse;
import com.kbase.backend.dto.PageResponse;
import com.kbase.backend.entity.Document;
import com.kbase.backend.entity.Project;
import com.kbase.backend.entity.User;
import com.kbase.backend.exception.AccessDeniedException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.repository.DocumentRepository;
import com.kbase.backend.service.MinioService;
import com.kbase.backend.service.ProjectService;
import com.kbase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "REST APIs cho upload, download, tìm kiếm và quản lý tập tin MinIO")
public class FileController {

    private final MinioService minioService;
    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final ProjectService projectService;

    public FileController(MinioService minioService,
                          DocumentRepository documentRepository,
                          UserService userService,
                          ProjectService projectService) {
        this.minioService = minioService;
        this.documentRepository = documentRepository;
        this.userService = userService;
        this.projectService = projectService;
    }

    // Upload tài liệu mới lên MinIO
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tải tập tin mới lên MinIO")
    public ResponseEntity<DocumentResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId,
            Authentication authentication) {

        User user = userService.getCurrentUser(authentication.getName());

        Project project = null;
        if (projectId != null) {
            project = projectService.getProjectOrThrow(projectId);
        }

        String storedFileName = minioService.uploadFile(file);

        Document doc = Document.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .project(project)
                .uploadedBy(user)
                .build();

        Document savedDoc = documentRepository.save(doc);
        return ResponseEntity.ok(DocumentResponse.fromEntity(savedDoc));
    }

    // Lấy danh sách tất cả tài liệu do người dùng hiện tại upload
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lấy danh sách tất cả tài liệu do tôi upload")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(Authentication authentication) {
        User user = userService.getCurrentUser(authentication.getName());
        List<Document> documentList = documentRepository.findByUploadedByOrderByCreatedAtDesc(user);

        List<DocumentResponse> responseList = new ArrayList<>();
        for (Document doc : documentList) {
            responseList.add(DocumentResponse.fromEntity(doc));
        }

        return ResponseEntity.ok(responseList);
    }

    // Lấy danh sách tài liệu thuộc một dự án
    @GetMapping(value = "/project/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lấy danh sách tài liệu theo dự án")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        Project project = projectService.getProjectOrThrow(projectId);

        List<Document> documentList = documentRepository.findByProjectOrderByCreatedAtDesc(project);
        List<DocumentResponse> responseList = new ArrayList<>();
        for (Document doc : documentList) {
            responseList.add(DocumentResponse.fromEntity(doc));
        }

        return ResponseEntity.ok(responseList);
    }

    // Tải tài liệu về máy
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Tải tài liệu về máy")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu không tồn tại với ID: " + id));

        InputStream inputStream = minioService.downloadFile(doc.getStoredFileName());
        InputStreamResource resource = new InputStreamResource(inputStream);

        String contentType = doc.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // Xóa tài liệu
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @Operation(summary = "Xóa tài liệu")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài liệu không tồn tại với ID: " + id));

        minioService.deleteFile(doc.getStoredFileName());
        documentRepository.delete(doc);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa tài liệu thành công!");
        return ResponseEntity.ok(response);
    }

    // Tìm kiếm và phân trang tài liệu người dùng theo từ khóa tên file
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tìm kiếm và phân trang tài liệu đồng nhất")
    public ResponseEntity<PageResponse<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {

        String[] sortParts = sort.split(",");
        org.springframework.data.domain.Sort.Direction direction = org.springframework.data.domain.Sort.Direction.DESC;
        if (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")) {
            direction = org.springframework.data.domain.Sort.Direction.ASC;
        }
        String sortBy = sortParts[0];

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(direction, sortBy)
        );

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        org.springframework.data.domain.Page<Document> docPage = documentRepository.searchAllDocuments(searchKeyword, pageable);

        List<DocumentResponse> items = new ArrayList<>();
        for (Document doc : docPage.getContent()) {
            items.add(DocumentResponse.fromEntity(doc));
        }

        PageResponse<DocumentResponse> response = PageResponse.<DocumentResponse>builder()
                .content(items)
                .pageNumber(docPage.getNumber())
                .pageSize(docPage.getSize())
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .isLast(docPage.isLast())
                .build();

        return ResponseEntity.ok(response);
    }
}