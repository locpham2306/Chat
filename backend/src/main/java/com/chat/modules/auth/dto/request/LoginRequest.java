package com.chat.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

// Nhận username và password FE gửi xuống khi đăng nhập.
public record LoginRequest(
        @NotBlank(message = "{validation.notBlank}") String username,
        @NotBlank(message = "{validation.notBlank}") String password) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=<redacted>]";
    }
}
