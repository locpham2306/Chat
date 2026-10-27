package com.chat.modules.conversation.mapper;

import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationPeerResponse;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public DirectConversationResponse toDirectResponse(ConversationEntity conversation) {
        return new DirectConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getCreatedAt());
    }

    public ConversationPeerResponse toPeerResponse(UserEntity user) {
        return new ConversationPeerResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getAvatarFileId(),
                UserStatusEnum.OFFLINE);
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

    public ConversationListItemResponse toListItemResponse(
            ConversationEntity conversation,
            long memberCount,
            ConversationPeerResponse peer,
            MessageResponse lastMessage,
            long unreadCount) {
        return new ConversationListItemResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getAvatarFileId(),
                conversation.getOwnerId(),
                memberCount,
                conversation.getCreatedAt(),
                peer,
                lastMessage,
                unreadCount);
    }

    public ConversationDetailResponse toDetailResponse(
            ConversationEntity conversation, long memberCount, ConversationPeerResponse peer) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getAvatarFileId(),
                conversation.getOwnerId(),
                memberCount,
                conversation.getCreatedAt(),
                peer);
    }
}
