package com.chat.modules.message.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.NoPermissionException;
import com.chat.exception.NotFoundException;
import com.chat.modules.conversation.entity.ConversationReadStateEntity;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationReadStateRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.dto.response.ConversationReadResponse;
import com.chat.modules.message.dto.response.MessageHistoryResponse;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.mapper.MessageMapper;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.message.service.ConversationMessageService;
import com.chat.modules.message.service.MessageReadEvent;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {
    private static final int MAX_HISTORY_LIMIT = 100;

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ConversationReadStateRepository readStates;
    private final MessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final FileRepository files;
    private final UserRepository users;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(
            UUID currentUserId, UUID conversationId, UUID before, Integer limit) {
        requireConversationAndMembership(currentUserId, conversationId);
        if (limit == null || limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new BadRequestException(ErrorConstants.VALIDATION_ERROR);
        }

        var page = PageRequest.of(0, limit + 1);
        List<MessageEntity> result;
        if (before == null) {
            result = messages.findByConversationIdOrderByCreatedAtDescIdDesc(conversationId, page);
        } else {
            var cursor = messages.findById(before)
                    .filter(message -> conversationId.equals(message.getConversationId()))
                    .orElseThrow(this::invalidCursor);
            result = messages.findHistoryBefore(
                    conversationId, cursor.getCreatedAt(), cursor.getId(), page);
        }

        boolean hasMore = result.size() > limit;
        var pageMessages = hasMore ? result.subList(0, limit) : result;
        var mapped = mapMessages(pageMessages);
        var data = pageMessages.stream().map(message -> mapped.get(message.getId())).toList();
        UUID nextCursor = hasMore && !pageMessages.isEmpty()
                ? pageMessages.get(pageMessages.size() - 1).getId() : null;
        return new MessageHistoryResponse(data, nextCursor, hasMore);
    }

    @Override
    @Transactional
    public ConversationReadResponse markRead(
            UUID currentUserId, UUID conversationId, UUID lastReadMessageId) {
        if (!conversations.existsById(conversationId)) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        var membership = members.findActiveForUpdate(conversationId, currentUserId)
                .orElseThrow(() -> new NoPermissionException(ErrorConstants.FORBIDDEN));
        var requestedMessage = messages.findById(lastReadMessageId)
                .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
        if (!conversationId.equals(requestedMessage.getConversationId())) {
            throw new BadRequestException(ErrorConstants.VALIDATION_ERROR);
        }

        var readState = readStates.findById(membership.getId()).orElse(null);
        MessageEntity actualMessage = requestedMessage;
        if (readState == null) {
            readState = new ConversationReadStateEntity(membership.getId(), requestedMessage.getId());
            readStates.saveAndFlush(readState);
        } else {
            var storedMessage = messages.findById(readState.getLastReadMessageId())
                    .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
            if (isAfter(requestedMessage, storedMessage)) {
                readState.advanceTo(requestedMessage.getId());
                readStates.saveAndFlush(readState);
            } else {
                actualMessage = storedMessage;
            }
        }

        long unreadCount = messages.countUnreadByConversationIds(
                        currentUserId, List.of(conversationId)).stream()
                .findFirst()
                .map(projection -> projection.getTotal())
                .orElse(0L);
        var response = new ConversationReadResponse(
                conversationId, currentUserId, actualMessage.getId(), unreadCount);
        events.publishEvent(new MessageReadEvent(
                response, members.findActiveUserIds(conversationId)));
        return response;
    }

    private void requireConversationAndMembership(UUID currentUserId, UUID conversationId) {
        if (!conversations.existsById(conversationId)) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, currentUserId)) {
            throw new NoPermissionException(ErrorConstants.FORBIDDEN);
        }
    }

    private boolean isAfter(MessageEntity candidate, MessageEntity current) {
        int createdAtOrder = candidate.getCreatedAt().compareTo(current.getCreatedAt());
        return createdAtOrder > 0
                || (createdAtOrder == 0
                && candidate.getId().toString().compareTo(current.getId().toString()) > 0);
    }

    private Map<UUID, MessageResponse> mapMessages(Collection<MessageEntity> messageEntities) {
        if (messageEntities.isEmpty()) {
            return Map.of();
        }
        var messageIds = messageEntities.stream().map(MessageEntity::getId).toList();
        var senderIds = messageEntities.stream().map(MessageEntity::getSenderId).distinct().toList();
        var sendersById = users.findAllById(senderIds).stream()
                .collect(Collectors.toMap(user -> user.getId(), Function.identity()));
        var attachmentLinks = attachments.findByMessageIdInOrderByMessageIdAscPositionAsc(messageIds);
        var fileIds = attachmentLinks.stream().map(link -> link.getFileId()).distinct().toList();
        var filesById = fileIds.isEmpty()
                ? Map.<UUID, FileEntity>of()
                : files.findAllById(fileIds).stream()
                .collect(Collectors.toMap(file -> file.getId(), Function.identity()));
        Map<UUID, List<FileEntity>> filesByMessageId = new HashMap<>();
        attachmentLinks.forEach(link -> {
            var file = filesById.get(link.getFileId());
            if (file != null) {
                filesByMessageId.computeIfAbsent(
                        link.getMessageId(), ignored -> new ArrayList<>()).add(file);
            }
        });
        return messageEntities.stream().collect(Collectors.toMap(
                MessageEntity::getId,
                message -> messageMapper.toResponse(
                        message,
                        Optional.ofNullable(sendersById.get(message.getSenderId()))
                                .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND)),
                        filesByMessageId.getOrDefault(message.getId(), List.of()))));
    }

    private BadRequestException invalidCursor() {
        return new BadRequestException(ErrorConstants.VALIDATION_ERROR);
    }
}
