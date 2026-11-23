package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Chứa mã và nội dung lỗi để BE gửi lên FE qua WebSocket.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketErrorData {
    private String clientMessageId;
    private String code;
    private String message;
}
