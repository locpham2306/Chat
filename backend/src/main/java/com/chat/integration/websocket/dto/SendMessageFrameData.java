package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

// Nhận dữ liệu FE gửi xuống BE qua WebSocket khi yêu cầu gửi tin nhắn.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class SendMessageFrameData {
    private UUID recipientId;
    private UUID conversationId;
    private String clientMessageId;
    private String content;
    private List<UUID> attachmentIds;
}
