package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Nhận ID người nhận để FE tìm cuộc hội thoại DIRECT với user đó.
public record DirectConversationRequest(
        @NotNull(message = "{validation.notNull}") UUID recipientId) {
}
