package com.chat.modules.websocket.dto;

import java.util.UUID;

public record TypingFrameData(UUID conversationId, Boolean isTyping) {
}
