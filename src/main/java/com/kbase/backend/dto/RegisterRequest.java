package com.kbase.backend.dto;

import com.kbase.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    /**
     * Role người dùng muốn đăng ký: ROLE_OWNER hoặc ROLE_USER.
     * Mặc định là ROLE_USER nếu không truyền.
     * Không thể đăng ký ROLE_ADMIN qua API này.
     */
    private Role role = Role.ROLE_USER;
}