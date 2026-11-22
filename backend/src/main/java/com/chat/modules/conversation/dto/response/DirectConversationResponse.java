package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin cơ bản của một cuộc hội thoại DIRECT để trả lên FE.
public record DirectConversationResponse(
        UUID id,
        ConversationTypeEnum type,
        Instant createdAt) {
}
