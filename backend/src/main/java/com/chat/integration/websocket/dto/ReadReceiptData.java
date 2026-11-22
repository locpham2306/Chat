package com.chat.integration.websocket.dto;

import java.util.UUID;

// Báo realtime cho FE rằng một user đã đọc tới tin nhắn nào.
public record ReadReceiptData(UUID conversationId, UUID userId, UUID lastReadMessageId) {
}
