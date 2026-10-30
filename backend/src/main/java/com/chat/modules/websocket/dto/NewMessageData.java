package com.chat.modules.websocket.dto;

import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.message.dto.response.MessageSenderResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NewMessageData(
        UUID id,
        UUID conversationId,
        UUID senderId,
        MessageSenderResponse sender,
        String clientMessageId,
        String content,
        List<FileResponse> attachments,
        Instant createdAt) {
}
