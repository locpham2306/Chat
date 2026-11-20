package com.chat.integration.websocket.dto;

import java.util.UUID;

// File này chứa dữ liệu BE gửi lên FE của các thành viên khác.
// Dùng để báo một người trong hội thoại đang nhập hoặc đã dừng nhập.
public record TypingData(
                UUID conversationId,
                UUID userId,
                RealtimeUserData user,
                boolean isTyping) {
}
