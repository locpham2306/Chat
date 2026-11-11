package com.chat.modules.websocket.dto;

public record WebSocketFrame<T>(String event, T data) {
}
