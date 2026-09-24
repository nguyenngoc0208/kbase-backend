package com.kbase.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "message", "Bạn đã truy cập endpoint bảo mật thành công!"));
    }
}