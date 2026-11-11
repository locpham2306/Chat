package com.chat.modules.message.service;

import com.chat.modules.message.dto.response.MessageHistoryResponse;
import com.chat.modules.message.dto.response.ConversationReadResponse;

import java.util.UUID;

public interface ConversationMessageService {
    MessageHistoryResponse getHistory(
            UUID currentUserId, UUID conversationId, UUID before, Integer limit);

    ConversationReadResponse markRead(
            UUID currentUserId, UUID conversationId, UUID lastReadMessageId);
}
