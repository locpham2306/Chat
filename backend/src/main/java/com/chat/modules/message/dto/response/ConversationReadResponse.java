package com.chat.modules.message.dto.response;

import java.util.UUID;

public record ConversationReadResponse(
        UUID conversationId,
        UUID userId,
        UUID lastReadMessageId,
        long unreadCount) {
}
