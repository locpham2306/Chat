package com.chat.modules.conversation.mapper;

import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationMapper {

    public DirectConversationResponse toDirectResponse(ConversationEntity conversation) {
        return new DirectConversationResponse(
                conversation.getId(),
                conversation.getType(),
                List.of(conversation.getDirectUserLowId(), conversation.getDirectUserHighId()),
                conversation.getCreatedAt());
    }

    public GroupConversationResponse toGroupResponse(ConversationEntity conversation, int memberCount) {
        return new GroupConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getAvatarFileId(),
                conversation.getOwnerId(),
                memberCount,
                conversation.getCreatedAt());
    }
}
