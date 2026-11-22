package com.chat.integration.websocket.dto;

// Chứa mã và nội dung lỗi để BE gửi lên FE qua WebSocket.
public record WebSocketErrorData(String clientMessageId, String code, String message) {
}
