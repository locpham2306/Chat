package com.chat.modules.websocket.dto;

import java.util.List;
import java.util.UUID;

public record SendMessageFrameData(
        UUID recipientId,
        UUID conversationId,
        String clientMessageId,
        String content,
        List<UUID> attachmentIds) {
}
