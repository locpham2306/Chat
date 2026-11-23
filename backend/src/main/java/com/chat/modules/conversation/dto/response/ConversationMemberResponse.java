package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.user.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Chứa thông tin user, vai trò và thời điểm tham gia của một thành viên trong nhóm.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberResponse {
    private UserResponse user;
    private MemberRoleEnum role;
    private Instant joinedAt;
}
