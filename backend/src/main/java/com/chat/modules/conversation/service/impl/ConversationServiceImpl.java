package com.chat.modules.conversation.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.NotFoundException;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversations;
    private final UserRepository users;
    private final ConversationMapper conversationMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<DirectConversationResponse> findDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("conversation.recipientIsCurrentUser");
        }
        if (!users.existsById(recipientId)) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        return conversations.findDirectBetweenUsers(
                        ConversationTypeEnum.DIRECT, currentUserId, recipientId)
                .map(conversationMapper::toDirectResponse);
    }
}
