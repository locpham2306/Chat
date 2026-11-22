package com.chat.integration.websocket.dto;

import java.time.Instant;
import java.util.UUID;

// Xác nhận với FE người gửi rằng tin nhắn đã được BE lưu thành công.
public record MessageAckData(
        String clientMessageId,
        UUID conversationId,
        UUID messageId,
        Instant createdAt) {
}
