package com.chat.modules.conversation.dto.response;

import java.util.List;
import java.util.UUID;

// Cho FE biết user nào vừa được thêm và user nào đã có sẵn trong nhóm.
public record AddConversationMembersResponse(
        List<UUID> addedUserIds,
        List<UUID> existingUserIds) {
}
