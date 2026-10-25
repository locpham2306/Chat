package com.chat.modules.conversation.service;

import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;

import java.util.Optional;
import java.util.UUID;

public interface ConversationService {
    Optional<DirectConversationResponse> findDirectConversation(UUID currentUserId, UUID recipientId);

    GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request);
}
