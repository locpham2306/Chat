package com.chat.integration.websocket.dto;

import java.util.List;
import java.util.UUID;

// File này định nghĩa dữ liệu FE gửi xuống BE khi yêu cầu gửi một tin nhắn.
public record SendMessageFrameData(
        UUID recipientId,
        UUID conversationId,
        String clientMessageId,
        String content,
        List<UUID> attachmentIds) {
}
