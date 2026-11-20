package com.chat.integration.websocket.dto;

// File này định nghĩa dữ liệu lỗi BE gửi lên FE qua WebSocket.
// clientMessageId giúp FE xác định tin gửi nào bị lỗi.
public record WebSocketErrorData(String clientMessageId, String code, String message) {
}
