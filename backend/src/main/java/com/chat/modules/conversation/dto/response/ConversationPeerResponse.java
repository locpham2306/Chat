package com.chat.modules.conversation.dto.response;

import com.chat.modules.user.enums.UserStatusEnum;

import java.util.UUID;

// Chứa thông tin người đang chat cùng user hiện tại trong hội thoại DIRECT.
public record ConversationPeerResponse(
        UUID id,
        String username,
        String fullName,
        UUID avatarFileId,
        UserStatusEnum status) {
}
