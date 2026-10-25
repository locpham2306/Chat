package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DirectConversationResponse(
        UUID id,
        ConversationTypeEnum type,
        List<UUID> memberIds,
        Instant createdAt) {
}
