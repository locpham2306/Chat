package com.chat.modules.websocket.dto;

import java.util.UUID;

public record ReadReceiptData(UUID conversationId, UUID userId, UUID lastReadMessageId) {
}
