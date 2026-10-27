package com.chat.modules.conversation.dto.response;

import com.chat.modules.user.enums.UserStatusEnum;

import java.util.UUID;

public record ConversationPeerResponse(
        UUID id,
        String username,
        String fullName,
        UUID avatarFileId,
        UserStatusEnum status) {
}
