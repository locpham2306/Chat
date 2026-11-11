package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.user.dto.response.UserResponse;

import java.time.Instant;

public record ConversationMemberResponse(
        UserResponse user,
        MemberRoleEnum role,
        Instant joinedAt) {
}
