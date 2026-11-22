package com.chat.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Nhận thông tin tài khoản FE gửi xuống khi đăng ký user mới.
public record RegisterRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 50, message = "{validation.maxLength}")
        String username,
        @NotBlank(message = "{validation.notBlank}")
        String password,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.maxLength}")
        String fullName) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=<redacted>, fullName=" + fullName + "]";
    }
}
