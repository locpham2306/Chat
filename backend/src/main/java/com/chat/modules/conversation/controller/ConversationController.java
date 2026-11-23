package com.chat.modules.conversation.controller;

import com.chat.common.dto.request.PaginationRequest;
import com.chat.common.dto.response.PageResponse;
import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.conversation.dto.request.AddConversationMembersRequest;
import com.chat.modules.conversation.dto.request.AvailableMembersRequest;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.DirectConversationRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.dto.response.AddConversationMembersResponse;
import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.dto.response.DirectConversationLookupResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.service.ConversationMemberService;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.message.dto.request.MarkConversationReadRequest;
import com.chat.modules.message.dto.request.MessageHistoryRequest;
import com.chat.modules.message.dto.response.ConversationReadResponse;
import com.chat.modules.message.dto.response.MessageHistoryResponse;
import com.chat.modules.message.service.ConversationMessageService;
import com.chat.modules.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final ConversationMemberService conversationMemberService;
    private final ConversationMessageService conversationMessageService;

    @GetMapping
    public BaseResponse<PageResponse<List<ConversationListItemResponse>>> getConversations(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @ModelAttribute PaginationRequest request) {
        UUID currentUserId = session.userId();
        PageResponse<List<ConversationListItemResponse>> conversations =
                conversationService.getConversations(
                        currentUserId, request.getPage(), request.getSize());
        return BaseResponse.success(conversations);
    }

    @GetMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> getConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id) {
        UUID currentUserId = session.userId();
        ConversationDetailResponse conversation =
                conversationService.getConversation(currentUserId, id);
        return BaseResponse.success(conversation);
    }

    @GetMapping("/{id}/members")
    public BaseResponse<PageResponse<List<ConversationMemberResponse>>> getConversationMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute PaginationRequest request) {
        UUID currentUserId = session.userId();
        PageResponse<List<ConversationMemberResponse>> members =
                conversationMemberService.getMembers(
                        currentUserId, id, request.getPage(), request.getSize());
        return BaseResponse.success(members);
    }

    @GetMapping("/{id}/messages")
    public BaseResponse<MessageHistoryResponse> getMessages(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute MessageHistoryRequest request) {
        UUID currentUserId = session.userId();
        MessageHistoryResponse history = conversationMessageService.getHistory(
                currentUserId, id, request.getBefore(), request.getLimit());
        return BaseResponse.success(history);
    }

    @PutMapping("/{id}/read")
    public BaseResponse<ConversationReadResponse> markRead(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody MarkConversationReadRequest request) {
        UUID currentUserId = session.userId();
        ConversationReadResponse readState = conversationMessageService.markRead(
                currentUserId, id, request.getLastReadMessageId());
        return BaseResponse.success(readState);
    }

    @PostMapping("/{id}/members")
    public BaseResponse<AddConversationMembersResponse> addConversationMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody AddConversationMembersRequest request) {
        UUID currentUserId = session.userId();
        AddConversationMembersResponse result =
                conversationMemberService.addMembers(currentUserId, id, request);
        return BaseResponse.success(result);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeConversationMember(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        UUID currentUserId = session.userId();
        conversationMemberService.removeMember(currentUserId, id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/available-members")
    public BaseResponse<PageResponse<List<UserResponse>>> getAvailableGroupMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute AvailableMembersRequest request) {
        UUID currentUserId = session.userId();
        PageResponse<List<UserResponse>> availableMembers =
                conversationMemberService.getAvailableMembers(
                        currentUserId, id, request.getKeyword(), request.getPage(), request.getSize());
        return BaseResponse.success(availableMembers);
    }

    @PutMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> updateConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConversationRequest request) {
        UUID currentUserId = session.userId();
        ConversationDetailResponse conversation =
                conversationService.updateConversation(currentUserId, id, request);
        return BaseResponse.success(conversation);
    }

    @GetMapping("/direct")
    public BaseResponse<DirectConversationLookupResponse> findDirectConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @ModelAttribute DirectConversationRequest request) {
        UUID currentUserId = session.userId();
        DirectConversationLookupResponse conversation =
                conversationService.findDirectConversation(currentUserId, request.getRecipientId());
        return BaseResponse.success(conversation);
    }

    @PostMapping("/group")
    public ResponseEntity<BaseResponse<GroupConversationResponse>> createGroup(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody CreateGroupRequest request) {
        UUID currentUserId = session.userId();
        GroupConversationResponse conversation =
                conversationService.createGroup(currentUserId, request);
        BaseResponse<GroupConversationResponse> response = BaseResponse.created(conversation);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}
