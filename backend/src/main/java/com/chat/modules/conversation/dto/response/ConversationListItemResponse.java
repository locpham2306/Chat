package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.message.dto.response.MessageResponse;

import java.time.Instant;
import java.util.UUID;

// Chứa dữ liệu một dòng hội thoại trong danh sách chat của FE.
public record ConversationListItemResponse(
        UUID id,
        ConversationTypeEnum type,
        String name,
        UUID avatarFileId,
        UUID ownerId,
        long memberCount,
        Instant createdAt,
        ConversationPeerResponse peer,
        MessageResponse lastMessage,
        long unreadCount) {
}
