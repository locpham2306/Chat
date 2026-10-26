package com.chat.modules.conversation.mapper;

import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationMemberMapper {
    private final UserMapper userMapper;

    public ConversationMemberResponse toResponse(
            ConversationMemberEntity membership, UserEntity user) {
        return new ConversationMemberResponse(
                userMapper.toResponse(user, UserStatusEnum.OFFLINE),
                membership.getRole(),
                membership.getJoinedAt());
    }
}
