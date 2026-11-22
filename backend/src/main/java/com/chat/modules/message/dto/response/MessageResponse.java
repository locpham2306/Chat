package com.chat.modules.message.dto.response;

import com.chat.modules.file.dto.response.FileResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Chứa toàn bộ dữ liệu một tin nhắn để BE trả lên FE.
public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageSenderResponse sender,
        String clientMessageId,
        String content,
        List<FileResponse> attachments,
        Instant createdAt) {
}
