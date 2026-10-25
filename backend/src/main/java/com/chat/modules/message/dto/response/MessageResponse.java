package com.chat.modules.message.dto.response;

import com.chat.modules.file.dto.response.FileResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String clientMessageId,
        String content,
        List<FileResponse> attachments,
        Instant createdAt) {
}
