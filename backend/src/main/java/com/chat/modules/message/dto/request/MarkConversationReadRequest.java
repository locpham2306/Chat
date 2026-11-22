package com.chat.modules.message.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Nhận ID tin nhắn cuối cùng user đã đọc trong một cuộc hội thoại.
public record MarkConversationReadRequest(
        @NotNull(message = "{validation.notNull}")
        UUID lastReadMessageId) {
}
