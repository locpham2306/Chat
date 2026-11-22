package com.chat.integration.websocket.dto;

// Bọc tên event và dữ liệu đi kèm trong một frame WebSocket chung.
public record WebSocketFrame<T>(String event, T data) {
}
