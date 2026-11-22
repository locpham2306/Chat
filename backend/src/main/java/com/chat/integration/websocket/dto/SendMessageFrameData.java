package com.chat.integration.websocket.dto;

import java.util.List;
import java.util.UUID;

// Nhận dữ liệu FE gửi xuống BE qua WebSocket khi yêu cầu gửi tin nhắn.
public record SendMessageFrameData(
        UUID recipientId,
        UUID conversationId,
        String clientMessageId,
        String content,
        List<UUID> attachmentIds) {
}
