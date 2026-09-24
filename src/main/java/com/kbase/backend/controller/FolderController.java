package com.kbase.backend.controller;

import com.kbase.backend.dto.FolderRequest;
import com.kbase.backend.dto.FolderResponse;
import com.kbase.backend.entity.Folder;
import com.kbase.backend.entity.User;
import com.kbase.backend.repository.FolderRepository;
import com.kbase.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/folders", produces = "application/json;charset=UTF-8")
public class FolderController {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    public FolderController(FolderRepository folderRepository, UserRepository userRepository) {
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderRequest request,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Folder folder = Folder.builder()
                .name(request.getName())
                .createdBy(user)
                .build();

        return ResponseEntity.ok(FolderResponse.fromEntity(folderRepository.save(folder)));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getMyFolders(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<FolderResponse> list = folderRepository.findByCreatedByOrderByCreatedAtDesc(user)
                .stream()
                .map(FolderResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFolder(@PathVariable Long id,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Thư mục không tồn tại"));

        if (!folder.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa thư mục này");
        }

        folderRepository.delete(folder);
        return ResponseEntity.ok(Map.of("message", "Xóa thư mục thành công!"));
    }
}