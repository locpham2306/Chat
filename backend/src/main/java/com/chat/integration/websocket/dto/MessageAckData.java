package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Xác nhận với FE người gửi rằng tin nhắn đã được BE lưu thành công.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageAckData {
    private String clientMessageId;
    private UUID conversationId;
    private UUID messageId;
    private Instant createdAt;
}
