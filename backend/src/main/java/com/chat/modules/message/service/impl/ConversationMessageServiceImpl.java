package com.chat.modules.message.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.modules.conversation.entity.ConversationReadStateEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationReadStateRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import com.chat.modules.message.dto.response.ConversationReadResponse;
import com.chat.modules.message.dto.response.MessageHistoryResponse;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.message.service.ConversationMessageService;
import com.chat.modules.message.service.MessageReadEvent;
import com.chat.modules.message.service.MessageResponseLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {
    private static final int MAX_HISTORY_LIMIT = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationReadStateRepository conversationReadStateRepository;
    private final MessageRepository messageRepository;
    private final MessageResponseLoader messageResponseLoader;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(
            UUID currentUserId, UUID conversationId, UUID before, Integer limit) {
        requireConversationAndMembership(currentUserId, conversationId);
        if (limit == null || limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw BusinessException.badRequest(ErrorConstants.VALIDATION_ERROR);
        }

        PageRequest page = PageRequest.of(0, limit + 1);
        List<MessageEntity> result;
        if (before == null) {
            result = messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(conversationId, page);
        } else {
            MessageEntity cursor = messageRepository.findById(before)
                    .filter(message -> conversationId.equals(message.getConversationId()))
                    .orElseThrow(this::invalidCursor);
            result = messageRepository.findHistoryBefore(
                    conversationId, cursor.getCreatedAt(), cursor.getId(), page);
        }

        boolean hasMore = result.size() > limit;
        List<MessageEntity> pageMessages = hasMore ? result.subList(0, limit) : result;
        Map<UUID, MessageResponse> mapped = messageResponseLoader.load(pageMessages);
        List<MessageResponse> data = pageMessages.stream()
                .map(message -> mapped.get(message.getId())).toList();
        UUID nextCursor = hasMore && !pageMessages.isEmpty()
                ? pageMessages.get(pageMessages.size() - 1).getId() : null;
        return new MessageHistoryResponse(data, nextCursor, hasMore);
    }

    @Override
    @Transactional
    public ConversationReadResponse markRead(
            UUID currentUserId, UUID conversationId, UUID lastReadMessageId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        ConversationMemberEntity membership = conversationMemberRepository
                .findActiveForUpdate(conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        MessageEntity requestedMessage = messageRepository.findById(lastReadMessageId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (!conversationId.equals(requestedMessage.getConversationId())) {
            throw BusinessException.badRequest(ErrorConstants.VALIDATION_ERROR);
        }

        ConversationReadStateEntity readState = conversationReadStateRepository
                .findById(membership.getId()).orElse(null);
        MessageEntity actualMessage = requestedMessage;
        if (readState == null) {
            readState = new ConversationReadStateEntity(membership.getId(), requestedMessage.getId());
            conversationReadStateRepository.saveAndFlush(readState);
        } else {
            MessageEntity storedMessage = messageRepository.findById(readState.getLastReadMessageId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
            if (isAfter(requestedMessage, storedMessage)) {
                readState.advanceTo(requestedMessage.getId());
                conversationReadStateRepository.saveAndFlush(readState);
            } else {
                actualMessage = storedMessage;
            }
        }

        List<ConversationCountProjection> unreadCounts = messageRepository.countUnreadByConversationIds(
                currentUserId, List.of(conversationId));
        long unreadCount = unreadCounts.isEmpty() ? 0L : unreadCounts.get(0).getTotal();
        ConversationReadResponse response = new ConversationReadResponse(
                conversationId, currentUserId, actualMessage.getId(), unreadCount);
        applicationEventPublisher.publishEvent(new MessageReadEvent(
                response, conversationMemberRepository.findActiveUserIds(conversationId)));
        return response;
    }

    private void requireConversationAndMembership(UUID currentUserId, UUID conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        if (!conversationMemberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, currentUserId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }

    private boolean isAfter(MessageEntity candidate, MessageEntity current) {
        int createdAtOrder = candidate.getCreatedAt().compareTo(current.getCreatedAt());
        return createdAtOrder > 0
                || (createdAtOrder == 0
                && candidate.getId().toString().compareTo(current.getId().toString()) > 0);
    }

    private BusinessException invalidCursor() {
        return BusinessException.badRequest(ErrorConstants.VALIDATION_ERROR);
    }
}
