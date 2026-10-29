package com.chat.modules.websocket.dto;

public record WebSocketErrorData(String clientMessageId, String code, String message) {
}
