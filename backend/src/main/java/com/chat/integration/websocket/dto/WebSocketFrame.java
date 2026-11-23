package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bọc tên event và dữ liệu đi kèm trong một frame WebSocket chung.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketFrame<T> {
    private String event;
    private T data;
}
