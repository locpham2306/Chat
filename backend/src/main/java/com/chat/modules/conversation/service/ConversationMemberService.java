package com.chat.modules.conversation.service;

import com.chat.common.dto.response.PageResponse;
import com.chat.modules.conversation.dto.request.AddConversationMembersRequest;
import com.chat.modules.conversation.dto.response.AddConversationMembersResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.user.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberService {
    PageResponse<List<ConversationMemberResponse>> getMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size);

    PageResponse<List<UserResponse>> getAvailableMembers(
            UUID currentUserId, UUID conversationId, String keyword, Integer page, Integer size);

    AddConversationMembersResponse addMembers(
            UUID currentUserId, UUID conversationId, AddConversationMembersRequest request);

    void removeMember(UUID currentUserId, UUID conversationId, UUID userId);
}
