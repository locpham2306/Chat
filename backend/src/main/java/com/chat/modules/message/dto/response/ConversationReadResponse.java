package com.chat.modules.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Trả mốc đọc mới và số tin chưa đọc của user trong cuộc hội thoại.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationReadResponse {
    private UUID conversationId;
    private UUID userId;
    private UUID lastReadMessageId;
    private long unreadCount;
}
