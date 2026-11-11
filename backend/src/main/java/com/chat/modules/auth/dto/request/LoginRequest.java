package com.chat.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{validation.notBlank}") String username,
        @NotBlank(message = "{validation.notBlank}") String password) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=<redacted>]";
    }
}
