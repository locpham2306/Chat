package com.chat.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Nhận họ tên và ảnh đại diện mới khi FE cập nhật hồ sơ user.
public record UpdateProfileRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.maxLength}")
        String fullName,
        @JsonProperty(value = "avatarFileId", required = true)
        UUID avatarFileId) {
}
