package com.chat.modules.conversation.dto.response;

import java.util.List;
import java.util.UUID;

public record AddConversationMembersResponse(
        List<UUID> addedUserIds,
        List<UUID> existingUserIds) {
}
