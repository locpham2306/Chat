package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AddConversationMembersRequest(
        @NotEmpty(message = "{validation.notEmpty}")
        List<@NotNull(message = "{validation.notNull}") UUID> memberIds) {
}
