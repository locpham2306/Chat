package com.chat.modules.conversation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

// Nhận tên và ảnh đại diện mới khi FE cập nhật thông tin nhóm.
@Getter
@EqualsAndHashCode
@ToString
public class UpdateConversationRequest {
    @NotBlank(message = "{validation.notBlank}")
    @Size(max = 100, message = "{validation.maxLength}")
    private final String name;

    private final UUID avatarFileId;

    @JsonCreator
    public UpdateConversationRequest(
            @JsonProperty("name") String name,
            @JsonProperty(value = "avatarFileId", required = true) UUID avatarFileId) {
        this.name = name;
        this.avatarFileId = avatarFileId;
    }
}
