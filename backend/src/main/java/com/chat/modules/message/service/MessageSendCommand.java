package com.chat.modules.message.service;

import java.util.List;
import java.util.UUID;

public record MessageSendCommand(
        UUID senderId,
        UUID recipientId,
        UUID conversationId,
        String clientMessageId,
        String content,
        List<UUID> attachmentIds) {

    public MessageSendCommand {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
}
