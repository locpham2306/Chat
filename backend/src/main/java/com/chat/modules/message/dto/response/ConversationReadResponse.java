package com.chat.modules.message.dto.response;

import java.util.UUID;

// Trả mốc đọc mới và số tin chưa đọc của user trong cuộc hội thoại.
public record ConversationReadResponse(
        UUID conversationId,
        UUID userId,
        UUID lastReadMessageId,
        long unreadCount) {
}
