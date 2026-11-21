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
        return BaseResponse.success(conversationService.getConversations(
                session.userId(), request.page(), request.size()));
    }

    @GetMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> getConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id) {
        return BaseResponse.success(conversationService.getConversation(session.userId(), id));
    }

    @GetMapping("/{id}/members")
    public BaseResponse<PageResponse<List<ConversationMemberResponse>>> getConversationMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute PaginationRequest request) {
        return BaseResponse.success(
                conversationMemberService.getMembers(
                        session.userId(), id, request.page(), request.size()));
    }

    @GetMapping("/{id}/messages")
    public BaseResponse<MessageHistoryResponse> getMessages(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute MessageHistoryRequest request) {
        return BaseResponse.success(
                conversationMessageService.getHistory(
                        session.userId(), id, request.before(), request.limit()));
    }

    @PutMapping("/{id}/read")
    public BaseResponse<ConversationReadResponse> markRead(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody MarkConversationReadRequest request) {
        return BaseResponse.success(conversationMessageService.markRead(
                session.userId(), id, request.lastReadMessageId()));
    }

    @PostMapping("/{id}/members")
    public BaseResponse<AddConversationMembersResponse> addConversationMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody AddConversationMembersRequest request) {
        return BaseResponse.success(
                conversationMemberService.addMembers(session.userId(), id, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeConversationMember(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        conversationMemberService.removeMember(session.userId(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/available-members")
    public BaseResponse<PageResponse<List<UserResponse>>> getAvailableGroupMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @ModelAttribute AvailableMembersRequest request) {
        return BaseResponse.success(
                conversationMemberService.getAvailableMembers(
                        session.userId(), id, request.keyword(), request.page(), request.size()));
    }

    @PutMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> updateConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return BaseResponse.success(conversationService.updateConversation(session.userId(), id, request));
    }

    @GetMapping("/direct")
    public BaseResponse<DirectConversationLookupResponse> findDirectConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @ModelAttribute DirectConversationRequest request) {
        return BaseResponse.success(
                conversationService.findDirectConversation(session.userId(), request.recipientId()));
    }

    @PostMapping("/group")
    public ResponseEntity<BaseResponse<GroupConversationResponse>> createGroup(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(conversationService.createGroup(session.userId(), request)));
    }
}
