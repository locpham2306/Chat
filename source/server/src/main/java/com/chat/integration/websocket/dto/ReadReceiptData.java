package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Báo realtime cho FE rằng một user đã đọc tới tin nhắn nào.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptData {
    private UUID conversationId;
    private UUID userId;
    private UUID lastReadMessageId;
}
