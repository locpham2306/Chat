package com.chat.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

// Nhận họ tên và ảnh đại diện mới khi FE cập nhật hồ sơ user.
@Getter
@EqualsAndHashCode
@ToString
public class UpdateProfileRequest {
    @NotBlank(message = "{validation.notBlank}")
    @Size(max = 100, message = "{validation.maxLength}")
    private final String fullName;

    private final UUID avatarFileId;

    @JsonCreator
    public UpdateProfileRequest(
            @JsonProperty("fullName") String fullName,
            @JsonProperty(value = "avatarFileId", required = true) UUID avatarFileId) {
        this.fullName = fullName;
        this.avatarFileId = avatarFileId;
    }
}
