package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.maxLength}")
        String name,
        @NotEmpty(message = "{validation.notEmpty}")
        List<@NotNull(message = "{validation.notNull}") UUID> memberIds,
        UUID avatarFileId) {
}
