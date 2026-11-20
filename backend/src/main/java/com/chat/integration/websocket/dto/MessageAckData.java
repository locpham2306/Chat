package com.chat.integration.websocket.dto;

import java.time.Instant;
import java.util.UUID;

// File này định nghĩa dữ liệu BE gửi lên FE người gửi để xác nhận gửi tin thành công.
public record MessageAckData(
        String clientMessageId,
        UUID conversationId,
        UUID messageId,
        Instant createdAt) {
}
