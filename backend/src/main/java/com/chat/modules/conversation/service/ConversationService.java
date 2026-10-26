package com.chat.modules.conversation.service;

import com.chat.common.dto.response.PageResponse;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationService {
    Optional<DirectConversationResponse> findDirectConversation(UUID currentUserId, UUID recipientId);

    GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request);

    PageResponse<List<ConversationListItemResponse>> getConversations(
            UUID currentUserId, Integer page, Integer size);

    ConversationDetailResponse getConversation(UUID currentUserId, UUID conversationId);

    ConversationDetailResponse updateConversation(
            UUID currentUserId, UUID conversationId, UpdateConversationRequest request);

    PageResponse<List<ConversationMemberResponse>> getConversationMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size);
}
