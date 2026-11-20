package com.chat.integration.websocket.dto;

// File này là lớp bọc chung cho dữ liệu BE gửi lên FE qua WebSocket.
// event cho FE biết loại sự kiện, data là dữ liệu thật đi kèm.
public record WebSocketFrame<T>(String event, T data) {
}
