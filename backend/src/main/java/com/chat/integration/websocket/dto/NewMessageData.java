package com.chat.integration.websocket.dto;

import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.message.dto.response.MessageSenderResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// File này định nghĩa dữ liệu tin nhắn mới BE gửi lên FE của các thành viên.
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
