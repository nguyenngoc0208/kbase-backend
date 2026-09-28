package com.kbase.backend.service;

import com.kbase.backend.dto.AuthResponse;
import com.kbase.backend.dto.LoginRequest;
import com.kbase.backend.dto.RegisterRequest;
import com.kbase.backend.dto.UserResponse;
import com.kbase.backend.entity.Role;
import com.kbase.backend.entity.User;
import com.kbase.backend.exception.AccessDeniedException;
import com.kbase.backend.exception.DuplicateResourceException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.repository.UserRepository;
import com.kbase.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng: " + request.getEmail());
        }

        // Chỉ cho phép đăng ký ROLE_OWNER hoặc ROLE_USER — Admin được tạo bằng DataInitializer
        Role role = request.getRole();
        if (role == null || role == Role.ROLE_ADMIN) {
            role = Role.ROLE_USER;
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AccessDeniedException("Email hoặc mật khẩu không chính xác!");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // ---- Admin operations ----

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        userRepository.delete(user);
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + email));
    }
}