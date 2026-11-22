package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin nhóm vừa được tạo để BE trả lên FE.
public record GroupConversationResponse(
        UUID id,
        ConversationTypeEnum type,
        String name,
        UUID avatarFileId,
        UUID ownerId,
        int memberCount,
        Instant createdAt) {
}
