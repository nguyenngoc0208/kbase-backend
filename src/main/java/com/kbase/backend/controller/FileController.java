package com.kbase.backend.controller;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kbase.backend.dto.DocumentResponse;
import com.kbase.backend.dto.PageResponse;
import com.kbase.backend.entity.Document;
import com.kbase.backend.entity.Folder;
import com.kbase.backend.entity.User;
import com.kbase.backend.repository.DocumentRepository;
import com.kbase.backend.repository.FolderRepository;
import com.kbase.backend.repository.UserRepository;
import com.kbase.backend.service.MinioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Controller", description = "Quản lý tài liệu và tập tin")
public class FileController {

        private final MinioService minioService;
        private final DocumentRepository documentRepository;
        private final UserRepository userRepository;
        private final FolderRepository folderRepository;

        public FileController(MinioService minioService,
                        DocumentRepository documentRepository,
                        UserRepository userRepository,
                        FolderRepository folderRepository) {
                this.minioService = minioService;
                this.documentRepository = documentRepository;
                this.userRepository = userRepository;
                this.folderRepository = folderRepository;
        }

        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Tải tài liệu lên hệ thống")
        public ResponseEntity<DocumentResponse> uploadFile(
                        @Parameter(description = "File đính kèm", required = true) @RequestPart("file") MultipartFile file,
                        @RequestParam(value = "folderId", required = false) Long folderId,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                Folder folder = null;
                if (folderId != null) {
                        folder = folderRepository.findById(folderId)
                                        .orElseThrow(() -> new RuntimeException("Thư mục không tồn tại"));

                        if (!folder.getCreatedBy().getId().equals(user.getId())) {
                                throw new RuntimeException("Bạn không có quyền tải file vào thư mục này");
                        }
                }

                String storedFileName = minioService.uploadFile(file);

                Document doc = Document.builder()
                                .originalFileName(file.getOriginalFilename())
                                .storedFileName(storedFileName)
                                .contentType(file.getContentType())
                                .fileSize(file.getSize())
                                .folder(folder)
                                .uploadedBy(user)
                                .build();

                Document savedDoc = documentRepository.save(doc);

                return ResponseEntity.ok(DocumentResponse.fromEntity(savedDoc));
        }

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Lấy danh sách tất cả tài liệu của tôi")
        public ResponseEntity<List<DocumentResponse>> getMyDocuments(Authentication authentication) {
                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                List<DocumentResponse> documents = documentRepository.findByUploadedByOrderByCreatedAtDesc(user)
                                .stream()
                                .map(DocumentResponse::fromEntity)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(documents);
        }

        @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        @Operation(summary = "Tải tài liệu về máy")
        public ResponseEntity<Resource> downloadDocument(
                        @PathVariable Long id,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                Document doc = documentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại"));

                if (!doc.getUploadedBy().getId().equals(user.getId())) {
                        throw new RuntimeException("Bạn không có quyền truy cập tài liệu này");
                }

                InputStream inputStream = minioService.downloadFile(doc.getStoredFileName());
                InputStreamResource resource = new InputStreamResource(inputStream);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + doc.getOriginalFileName() + "\"")
                                .contentType(MediaType
                                                .parseMediaType(doc.getContentType() != null ? doc.getContentType()
                                                                : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                                .body(resource);
        }

        @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Xóa tài liệu")
        public ResponseEntity<Map<String, String>> deleteDocument(
                        @PathVariable Long id,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                Document doc = documentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại"));

                if (!doc.getUploadedBy().getId().equals(user.getId())) {
                        throw new RuntimeException("Bạn không có quyền xóa tài liệu này");
                }

                minioService.deleteFile(doc.getStoredFileName());
                documentRepository.delete(doc);

                return ResponseEntity.ok(Map.of("message", "Xóa tài liệu thành công!"));
        }

        @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Tìm kiếm và phân trang tài liệu")
        public ResponseEntity<PageResponse<DocumentResponse>> searchDocuments(
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "createdAt,desc") String sort,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                String[] sortParts = sort.split(",");
                org.springframework.data.domain.Sort.Direction direction = sortParts.length > 1
                                && sortParts[1].equalsIgnoreCase("asc")
                                                ? org.springframework.data.domain.Sort.Direction.ASC
                                                : org.springframework.data.domain.Sort.Direction.DESC;
                String sortBy = sortParts[0];

                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                                size,
                                org.springframework.data.domain.Sort.by(direction, sortBy));

                org.springframework.data.domain.Page<Document> docPage = documentRepository.searchMyDocuments(user,
                                (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null, pageable);

                List<DocumentResponse> items = docPage.getContent().stream()
                                .map(DocumentResponse::fromEntity)
                                .collect(Collectors.toList());

                PageResponse<DocumentResponse> response = PageResponse
                                .<DocumentResponse>builder()
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