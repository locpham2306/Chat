package com.chat.modules.message.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkConversationReadRequest(
        @NotNull(message = "{validation.notNull}")
        UUID lastReadMessageId) {
}
