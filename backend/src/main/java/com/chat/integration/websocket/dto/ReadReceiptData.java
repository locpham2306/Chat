package com.chat.integration.websocket.dto;

import java.util.UUID;

// File này chứa dữ liệu BE gửi lên FE để báo một user đã đọc tới tin nhắn nào.
public record ReadReceiptData(UUID conversationId, UUID userId, UUID lastReadMessageId) {
}
