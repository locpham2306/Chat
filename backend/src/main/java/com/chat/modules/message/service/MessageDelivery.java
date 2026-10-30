package com.chat.modules.message.service;

import com.chat.modules.file.dto.response.FileResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDelivery(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderFullName,
        UUID senderAvatarFileId,
        String clientMessageId,
        String content,
        List<FileResponse> attachments,
        Instant createdAt,
        List<UUID> activeMemberIds,
        boolean newlyCreated) {
}
