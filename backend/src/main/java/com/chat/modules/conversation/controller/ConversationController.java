package com.chat.modules.conversation.controller;

import com.chat.common.wrapper.BaseResponse;
import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversations;

    @GetMapping("/direct")
    public ResponseEntity<BaseResponse<DirectConversationResponse>> findDirectConversation(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @RequestParam UUID recipientId) {
        var conversation = conversations.findDirectConversation(session.userId(), recipientId);
        return conversation
                .map(value -> ResponseEntity.ok(BaseResponse.success(value)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/group")
    public ResponseEntity<BaseResponse<GroupConversationResponse>> createGroup(
            @RequestAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE) SessionIdentity session,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(conversations.createGroup(session.userId(), request)));
    }
}
