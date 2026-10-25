package com.chat.modules.conversation.repository.projection;

import java.util.UUID;

public interface ConversationCountProjection {
    UUID getConversationId();

    long getTotal();
}
