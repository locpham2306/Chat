package com.chat.modules.message.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
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
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
        MessageSendCommand command = validate(rawCommand);
        ConversationEntity conversation;
        MessageEntity existing;
        UserEntity sender;

        if (command.recipientId() != null) {
            sender = lockDirectUsers(command.senderId(), command.recipientId());
            conversation = conversations.findDirectBetweenUsers(
                    ConversationTypeEnum.DIRECT, command.senderId(), command.recipientId()).orElse(null);
            existing = messages.findBySenderIdAndClientMessageId(
                    command.senderId(), command.clientMessageId()).orElse(null);
            if (existing != null) {
                if (conversation == null || !conversation.getId().equals(existing.getConversationId())) {
                    throw conflict();
                }
                return retry(existing, command, sender);
            }
            if (conversation == null) {
                conversation = createDirect(command.senderId(), command.recipientId());
            } else {
                requireActiveMember(conversation.getId(), command.senderId());
            }
        } else {
            sender = lockUser(command.senderId());
            existing = messages.findBySenderIdAndClientMessageId(
                    command.senderId(), command.clientMessageId()).orElse(null);
            if (existing != null) {
                if (!command.conversationId().equals(existing.getConversationId())) {
                    throw conflict();
                }
                return retry(existing, command, sender);
            }
            conversation = conversations.findByIdForUpdate(command.conversationId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
            requireActiveMember(conversation.getId(), command.senderId());
        }

        List<FileEntity> orderedFiles = resolveAndBindFiles(
                command.senderId(), conversation.getId(), command.attachmentIds());
        MessageEntity message = messages.saveAndFlush(new MessageEntity(
                conversation.getId(), command.senderId(), command.clientMessageId(), command.content()));
        if (!orderedFiles.isEmpty()) {
            ArrayList<MessageAttachmentEntity> links = new ArrayList<>(orderedFiles.size());
            for (int index = 0; index < orderedFiles.size(); index++) {
                links.add(new MessageAttachmentEntity(message.getId(), orderedFiles.get(index).getId(), index));
            }
            attachments.saveAllAndFlush(links);
        }
        return delivery(
                message, sender, orderedFiles,
                members.findActiveUserIds(conversation.getId()), true);
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
        List<UUID> attachmentIds = command.attachmentIds();
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

    private UserEntity lockDirectUsers(UUID firstUserId, UUID secondUserId) {
        boolean firstIsLow = firstUserId.toString().compareTo(secondUserId.toString()) < 0;
        UserEntity lowUser = lockUser(firstIsLow ? firstUserId : secondUserId);
        UserEntity highUser = lockUser(firstIsLow ? secondUserId : firstUserId);
        return firstIsLow ? lowUser : highUser;
    }

    private UserEntity lockUser(UUID userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }

    private ConversationEntity createDirect(UUID senderId, UUID recipientId) {
        ConversationEntity conversation = conversations.saveAndFlush(
                ConversationEntity.direct(senderId, recipientId));
        Instant joinedAt = clock.instant();
        members.saveAllAndFlush(List.of(
                new ConversationMemberEntity(conversation.getId(), senderId, MemberRoleEnum.MEMBER, joinedAt),
                new ConversationMemberEntity(conversation.getId(), recipientId, MemberRoleEnum.MEMBER, joinedAt)));
        return conversation;
    }

    private void requireActiveMember(UUID conversationId, UUID userId) {
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }

    private List<FileEntity> resolveAndBindFiles(
            UUID senderId, UUID conversationId, List<UUID> attachmentIds) {
        if (attachmentIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, FileEntity> filesById = new HashMap<>();
        for (FileEntity file : files.findAllByIdForUpdate(attachmentIds)) {
            filesById.put(file.getId(), file);
        }
        if (filesById.size() != attachmentIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }

        List<FileEntity> orderedFiles = new ArrayList<>(attachmentIds.size());
        for (UUID attachmentId : attachmentIds) {
            FileEntity file = filesById.get(attachmentId);
            if (!senderId.equals(file.getUploaderId())
                    || (file.getConversationId() != null
                    && !conversationId.equals(file.getConversationId()))) {
                throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
            }
            file.bindToConversation(conversationId);
            orderedFiles.add(file);
        }
        files.saveAllAndFlush(orderedFiles);
        return orderedFiles;
    }

    private MessageDelivery retry(
            MessageEntity existing, MessageSendCommand command, UserEntity sender) {
        List<MessageAttachmentEntity> links =
                attachments.findByMessageIdOrderByPositionAsc(existing.getId());
        List<UUID> storedAttachmentIds = links.stream()
                .map(MessageAttachmentEntity::getFileId).toList();
        if (!Objects.equals(existing.getContent(), command.content())
                || !storedAttachmentIds.equals(command.attachmentIds())) {
            throw conflict();
        }
        Map<UUID, FileEntity> filesById = new HashMap<>();
        for (FileEntity file : files.findAllById(storedAttachmentIds)) {
            filesById.put(file.getId(), file);
        }

        List<FileEntity> orderedFiles = new ArrayList<>(storedAttachmentIds.size());
        for (UUID attachmentId : storedAttachmentIds) {
            FileEntity file = filesById.get(attachmentId);
            if (file == null) {
                throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
            }
            orderedFiles.add(file);
        }
        return delivery(existing, sender, orderedFiles, List.of(), false);
    }

    private MessageDelivery delivery(
            MessageEntity message,
            UserEntity sender,
            Collection<FileEntity> attachedFiles,
            List<UUID> activeMemberIds,
            boolean newlyCreated) {
        List<FileResponse> fileResponses = attachedFiles.stream()
                .map(file -> new FileResponse(
                        file.getId(), file.getName(), file.getContentType(), file.getSize(),
                        "/api/files/" + file.getId()))
                .toList();
        return new MessageDelivery(
                message.getId(), message.getConversationId(), message.getSenderId(),
                sender.getFullName(), sender.getAvatarFileId(),
                message.getClientMessageId(), message.getContent(), fileResponses,
                message.getCreatedAt(), List.copyOf(activeMemberIds), newlyCreated);
    }

    private BusinessException invalid() {
        return BusinessException.badRequest(ErrorConstants.VALIDATION_ERROR);
    }

    private BusinessException conflict() {
        return BusinessException.conflict(ErrorConstants.CONFLICT);
    }
}
