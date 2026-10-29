package com.chat.modules.message.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.ConflictException;
import com.chat.exception.NoPermissionException;
import com.chat.exception.NotFoundException;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.entity.MessageAttachmentEntity;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.message.service.MessageDelivery;
import com.chat.modules.message.service.MessageSendCommand;
import com.chat.modules.message.service.MessageService;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final FileRepository files;
    private final UserRepository users;
    private final Clock clock;

    @Override
    @Transactional
    public MessageDelivery send(MessageSendCommand rawCommand) {
        var command = validate(rawCommand);
        ConversationEntity conversation;
        MessageEntity existing;

        if (command.recipientId() != null) {
            lockDirectUsers(command.senderId(), command.recipientId());
            conversation = conversations.findDirectBetweenUsers(
                    ConversationTypeEnum.DIRECT, command.senderId(), command.recipientId()).orElse(null);
            existing = messages.findBySenderIdAndClientMessageId(
                    command.senderId(), command.clientMessageId()).orElse(null);
            if (existing != null) {
                if (conversation == null || !conversation.getId().equals(existing.getConversationId())) {
                    throw conflict();
                }
                return retry(existing, command);
            }
            if (conversation == null) {
                conversation = createDirect(command.senderId(), command.recipientId());
            } else {
                requireActiveMember(conversation.getId(), command.senderId());
            }
        } else {
            lockUser(command.senderId());
            existing = messages.findBySenderIdAndClientMessageId(
                    command.senderId(), command.clientMessageId()).orElse(null);
            if (existing != null) {
                if (!command.conversationId().equals(existing.getConversationId())) {
                    throw conflict();
                }
                return retry(existing, command);
            }
            conversation = conversations.findByIdForUpdate(command.conversationId())
                    .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
            requireActiveMember(conversation.getId(), command.senderId());
        }

        var orderedFiles = resolveAndBindFiles(
                command.senderId(), conversation.getId(), command.attachmentIds());
        var message = messages.saveAndFlush(new MessageEntity(
                conversation.getId(), command.senderId(), command.clientMessageId(), command.content()));
        if (!orderedFiles.isEmpty()) {
            var links = new ArrayList<MessageAttachmentEntity>(orderedFiles.size());
            for (int index = 0; index < orderedFiles.size(); index++) {
                links.add(new MessageAttachmentEntity(message.getId(), orderedFiles.get(index).getId(), index));
            }
            attachments.saveAllAndFlush(links);
        }
        return delivery(message, orderedFiles, members.findActiveUserIds(conversation.getId()), true);
    }

    private MessageSendCommand validate(MessageSendCommand command) {
        if (command == null || command.senderId() == null) {
            throw invalid();
        }
        boolean hasRecipient = command.recipientId() != null;
        boolean hasConversation = command.conversationId() != null;
        if (hasRecipient == hasConversation) {
            throw invalid();
        }
        String clientMessageId = command.clientMessageId() == null
                ? null : command.clientMessageId().trim();
        if (clientMessageId == null || clientMessageId.isEmpty() || clientMessageId.length() > 128) {
            throw invalid();
        }
        String content = command.content();
        if (content != null && content.isBlank()) {
            content = null;
        }
        var attachmentIds = command.attachmentIds();
        if (attachmentIds.stream().anyMatch(Objects::isNull)
                || new LinkedHashSet<>(attachmentIds).size() != attachmentIds.size()) {
            throw invalid();
        }
        if (content == null && attachmentIds.isEmpty()) {
            throw invalid();
        }
        if (hasRecipient && command.senderId().equals(command.recipientId())) {
            throw invalid();
        }
        return new MessageSendCommand(
                command.senderId(), command.recipientId(), command.conversationId(),
                clientMessageId, content, attachmentIds);
    }

    private void lockDirectUsers(UUID firstUserId, UUID secondUserId) {
        boolean firstIsLow = firstUserId.toString().compareTo(secondUserId.toString()) < 0;
        lockUser(firstIsLow ? firstUserId : secondUserId);
        lockUser(firstIsLow ? secondUserId : firstUserId);
    }

    private void lockUser(UUID userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
    }

    private ConversationEntity createDirect(UUID senderId, UUID recipientId) {
        var conversation = conversations.saveAndFlush(ConversationEntity.direct(senderId, recipientId));
        var joinedAt = clock.instant();
        members.saveAllAndFlush(List.of(
                new ConversationMemberEntity(conversation.getId(), senderId, MemberRoleEnum.MEMBER, joinedAt),
                new ConversationMemberEntity(conversation.getId(), recipientId, MemberRoleEnum.MEMBER, joinedAt)));
        return conversation;
    }

    private void requireActiveMember(UUID conversationId, UUID userId) {
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
            throw new NoPermissionException(ErrorConstants.FORBIDDEN);
        }
    }

    private List<FileEntity> resolveAndBindFiles(
            UUID senderId, UUID conversationId, List<UUID> attachmentIds) {
        if (attachmentIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, FileEntity> filesById = files.findAllByIdForUpdate(attachmentIds).stream()
                .collect(Collectors.toMap(FileEntity::getId, Function.identity()));
        if (filesById.size() != attachmentIds.size()) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        var orderedFiles = attachmentIds.stream().map(filesById::get).toList();
        for (var file : orderedFiles) {
            if (!senderId.equals(file.getUploaderId())
                    || (file.getConversationId() != null
                    && !conversationId.equals(file.getConversationId()))) {
                throw new NoPermissionException(ErrorConstants.FORBIDDEN);
            }
            file.bindToConversation(conversationId);
        }
        files.saveAllAndFlush(orderedFiles);
        return orderedFiles;
    }

    private MessageDelivery retry(MessageEntity existing, MessageSendCommand command) {
        var links = attachments.findByMessageIdOrderByPositionAsc(existing.getId());
        var storedAttachmentIds = links.stream().map(MessageAttachmentEntity::getFileId).toList();
        if (!Objects.equals(existing.getContent(), command.content())
                || !storedAttachmentIds.equals(command.attachmentIds())) {
            throw conflict();
        }
        Map<UUID, FileEntity> filesById = files.findAllById(storedAttachmentIds).stream()
                .collect(Collectors.toMap(FileEntity::getId, Function.identity()));
        var orderedFiles = storedAttachmentIds.stream()
                .map(filesById::get)
                .filter(Objects::nonNull)
                .toList();
        if (orderedFiles.size() != storedAttachmentIds.size()) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        return delivery(existing, orderedFiles, List.of(), false);
    }

    private MessageDelivery delivery(
            MessageEntity message,
            Collection<FileEntity> attachedFiles,
            List<UUID> activeMemberIds,
            boolean newlyCreated) {
        var fileResponses = attachedFiles.stream()
                .map(file -> new FileResponse(
                        file.getId(), file.getName(), file.getContentType(), file.getSize(),
                        "/api/files/" + file.getId()))
                .toList();
        return new MessageDelivery(
                message.getId(), message.getConversationId(), message.getSenderId(),
                message.getClientMessageId(), message.getContent(), fileResponses,
                message.getCreatedAt(), List.copyOf(activeMemberIds), newlyCreated);
    }

    private BadRequestException invalid() {
        return new BadRequestException(ErrorConstants.VALIDATION_ERROR);
    }

    private ConflictException conflict() {
        return new ConflictException(ErrorConstants.CONFLICT);
    }
}
