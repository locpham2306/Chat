package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DirectConversationRequest(
        @NotNull(message = "{validation.notNull}") UUID recipientId) {
}
