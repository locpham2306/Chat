package com.chat.modules.websocket.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageAckData(
        String clientMessageId,
        UUID conversationId,
        UUID messageId,
        Instant createdAt) {
}
