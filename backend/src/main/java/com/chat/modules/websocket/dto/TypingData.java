package com.chat.modules.websocket.dto;

import java.util.UUID;

public record TypingData(UUID conversationId, UUID userId, boolean isTyping) {
}
