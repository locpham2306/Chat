package com.chat.modules.conversation.controller;

import com.chat.common.dto.response.PageResponse;
import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.dto.response.DirectConversationLookupResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversations;

    @GetMapping
    public BaseResponse<PageResponse<List<ConversationListItemResponse>>> getConversations(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{validation.min}") Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{validation.min}")
            @Max(value = 100, message = "{validation.max}") Integer size) {
        return BaseResponse.success(conversations.getConversations(session.userId(), page, size));
    }

    @GetMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> getConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id) {
        return BaseResponse.success(conversations.getConversation(session.userId(), id));
    }

    @GetMapping("/{id}/members")
    public BaseResponse<PageResponse<List<ConversationMemberResponse>>> getConversationMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{validation.min}") Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{validation.min}")
            @Max(value = 100, message = "{validation.max}") Integer size) {
        return BaseResponse.success(
                conversations.getConversationMembers(session.userId(), id, page, size));
    }

    @GetMapping("/{id}/available-members")
    public BaseResponse<PageResponse<List<UserResponse>>> getAvailableGroupMembers(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{validation.min}") Integer page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{validation.min}")
            @Max(value = 100, message = "{validation.max}") Integer size) {
        return BaseResponse.success(
                conversations.getAvailableGroupMembers(session.userId(), id, keyword, page, size));
    }

    @PutMapping("/{id}")
    public BaseResponse<ConversationDetailResponse> updateConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConversationRequest request) {
        return BaseResponse.success(conversations.updateConversation(session.userId(), id, request));
    }

    @GetMapping("/direct")
    public BaseResponse<DirectConversationLookupResponse> findDirectConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @RequestParam UUID recipientId) {
        return BaseResponse.success(conversations.findDirectConversation(session.userId(), recipientId));
    }

    @PostMapping("/group")
    public ResponseEntity<BaseResponse<GroupConversationResponse>> createGroup(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(conversations.createGroup(session.userId(), request)));
    }
}
