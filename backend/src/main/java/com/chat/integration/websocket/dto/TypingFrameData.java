package com.chat.integration.websocket.dto;

import java.util.UUID;

// Nhận trạng thái đang nhập mà FE gửi xuống BE qua WebSocket.
public record TypingFrameData(UUID conversationId, Boolean isTyping) {
}
