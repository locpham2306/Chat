package com.chat.modules.conversation.service;

import com.chat.common.dto.response.PageResponse;
import com.chat.modules.conversation.dto.request.AddConversationMembersRequest;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.dto.response.AddConversationMembersResponse;
import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.dto.response.DirectConversationLookupResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.user.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface ConversationService {
    DirectConversationLookupResponse findDirectConversation(UUID currentUserId, UUID recipientId);

    GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request);

    PageResponse<List<ConversationListItemResponse>> getConversations(
            UUID currentUserId, Integer page, Integer size);

    ConversationDetailResponse getConversation(UUID currentUserId, UUID conversationId);

    ConversationDetailResponse updateConversation(
            UUID currentUserId, UUID conversationId, UpdateConversationRequest request);

    PageResponse<List<ConversationMemberResponse>> getConversationMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size);

    PageResponse<List<UserResponse>> getAvailableGroupMembers(
            UUID currentUserId, UUID conversationId, String keyword, Integer page, Integer size);

    AddConversationMembersResponse addConversationMembers(
            UUID currentUserId, UUID conversationId, AddConversationMembersRequest request);

    void removeConversationMember(UUID currentUserId, UUID conversationId, UUID userId);
}
