package com.chat.modules.conversation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateConversationRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.maxLength}")
        String name,
        @JsonProperty(value = "avatarFileId", required = true)
        UUID avatarFileId) {
}
