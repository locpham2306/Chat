package com.chat.integration.websocket.dto;

import java.util.UUID;

// Báo realtime cho FE rằng một người trong hội thoại đang nhập hoặc đã dừng nhập.
public record TypingData(
                UUID conversationId,
                UUID userId,
                RealtimeUserData user,
                boolean isTyping) {
}
