package com.kbase.backend.config;

import com.kbase.backend.entity.Role;
import com.kbase.backend.entity.User;
import com.kbase.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tự động seed dữ liệu khởi đầu khi ứng dụng bắt đầu.
 * - Tạo tài khoản ADMIN mặc định nếu chưa tồn tại.
 *
 * Tài khoản admin mặc định:
 *   Email   : admin@kbase.com
 *   Password: admin123
 *
 * Nên đổi mật khẩu ngay sau khi deploy lần đầu!
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@kbase.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ [DataInitializer] Đã tạo tài khoản Admin: " + adminEmail);
            } else {
                System.out.println("ℹ️ [DataInitializer] Tài khoản Admin đã tồn tại: " + adminEmail);
            }
        };
    }
}
