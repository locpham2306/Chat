package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin chi tiết của một cuộc hội thoại để trả lên màn hình chi tiết của FE.
public record ConversationDetailResponse(
        UUID id,
        ConversationTypeEnum type,
        String name,
        UUID avatarFileId,
        UUID ownerId,
        long memberCount,
        Instant createdAt,
        ConversationPeerResponse peer) {
}
