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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
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
            Optional<ConversationEntity> directConversation =
                    conversationRepository.findDirectBetweenUsers(
                            ConversationTypeEnum.DIRECT,
                            command.senderId(),
                            command.recipientId());
            conversation = directConversation.orElse(null);

            Optional<MessageEntity> existingMessage =
                    messageRepository.findBySenderIdAndClientMessageId(
                            command.senderId(), command.clientMessageId());
            existing = existingMessage.orElse(null);
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
            Optional<MessageEntity> existingMessage =
                    messageRepository.findBySenderIdAndClientMessageId(
                            command.senderId(), command.clientMessageId());
            existing = existingMessage.orElse(null);
            if (existing != null) {
                if (!command.conversationId().equals(existing.getConversationId())) {
                    throw conflict();
                }
                return retry(existing, command, sender);
            }
            conversation = conversationRepository.findByIdForUpdate(command.conversationId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
            requireActiveMember(conversation.getId(), command.senderId());
        }

        List<FileEntity> orderedFiles = resolveAndBindFiles(
                command.senderId(), conversation.getId(), command.attachmentIds());
        MessageEntity newMessage = new MessageEntity(
                conversation.getId(),
                command.senderId(),
                command.clientMessageId(),
                command.content());
        MessageEntity message = messageRepository.saveAndFlush(newMessage);
        if (!orderedFiles.isEmpty()) {
            ArrayList<MessageAttachmentEntity> links = new ArrayList<>(orderedFiles.size());
            for (int index = 0; index < orderedFiles.size(); index++) {
                FileEntity file = orderedFiles.get(index);
                MessageAttachmentEntity link = new MessageAttachmentEntity(
                        message.getId(), file.getId(), index);
                links.add(link);
            }
            messageAttachmentRepository.saveAllAndFlush(links);
        }
        return delivery(
                message, sender, orderedFiles,
                conversationMemberRepository.findActiveUserIds(conversation.getId()), true);
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
        String clientMessageId = command.clientMessageId();
        if (clientMessageId != null) {
            clientMessageId = clientMessageId.trim();
        }
        if (clientMessageId == null || clientMessageId.isEmpty() || clientMessageId.length() > 128) {
            throw invalid();
        }
        String content = command.content();
        if (content != null && content.isBlank()) {
            content = null;
        }
        List<UUID> attachmentIds = command.attachmentIds();
        boolean containsNullAttachmentId = false;
        for (UUID attachmentId : attachmentIds) {
            if (attachmentId == null) {
                containsNullAttachmentId = true;
                break;
            }
        }
        boolean containsDuplicateAttachmentId =
                new LinkedHashSet<>(attachmentIds).size() != attachmentIds.size();
        if (containsNullAttachmentId || containsDuplicateAttachmentId) {
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
        UUID lowUserId = secondUserId;
        UUID highUserId = firstUserId;
        if (firstIsLow) {
            lowUserId = firstUserId;
            highUserId = secondUserId;
        }

        UserEntity lowUser = lockUser(lowUserId);
        UserEntity highUser = lockUser(highUserId);
        if (firstIsLow) {
            return lowUser;
        }
        return highUser;
    }

    private UserEntity lockUser(UUID userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }

    private ConversationEntity createDirect(UUID senderId, UUID recipientId) {
        if (senderId == null || recipientId == null) {
            throw new IllegalArgumentException("Sender and recipient are required");
        }
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("A direct conversation requires two different users");
        }

        // Chuỗi UUID chuẩn có cùng thứ tự byte không dấu với BINARY(16) trong MySQL.
        boolean senderIsLow = senderId.toString().compareTo(recipientId.toString()) < 0;
        UUID lowUserId = recipientId;
        UUID highUserId = senderId;
        if (senderIsLow) {
            lowUserId = senderId;
            highUserId = recipientId;
        }

        ConversationEntity conversation = new ConversationEntity();
        conversation.setType(ConversationTypeEnum.DIRECT);
        conversation.setDirectUserLowId(lowUserId);
        conversation.setDirectUserHighId(highUserId);
        conversation = conversationRepository.saveAndFlush(conversation);

        Instant joinedAt = clock.instant();
        ConversationMemberEntity senderMembership = new ConversationMemberEntity(
                conversation.getId(), senderId, MemberRoleEnum.MEMBER, joinedAt);
        ConversationMemberEntity recipientMembership = new ConversationMemberEntity(
                conversation.getId(), recipientId, MemberRoleEnum.MEMBER, joinedAt);
        List<ConversationMemberEntity> memberships =
                List.of(senderMembership, recipientMembership);
        conversationMemberRepository.saveAllAndFlush(memberships);
        return conversation;
    }

    private void requireActiveMember(UUID conversationId, UUID userId) {
        if (!conversationMemberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, userId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }

    private List<FileEntity> resolveAndBindFiles(
            UUID senderId, UUID conversationId, List<UUID> attachmentIds) {
        if (attachmentIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, FileEntity> filesById = new HashMap<>();
        for (FileEntity file : fileRepository.findAllByIdForUpdate(attachmentIds)) {
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
        fileRepository.saveAllAndFlush(orderedFiles);
        return orderedFiles;
    }

    private MessageDelivery retry(
            MessageEntity existing, MessageSendCommand command, UserEntity sender) {
        List<MessageAttachmentEntity> links =
                messageAttachmentRepository.findByMessageIdOrderByPositionAsc(existing.getId());
        List<UUID> storedAttachmentIds = new ArrayList<>(links.size());
        for (MessageAttachmentEntity link : links) {
            storedAttachmentIds.add(link.getFileId());
        }
        if (!Objects.equals(existing.getContent(), command.content())
                || !storedAttachmentIds.equals(command.attachmentIds())) {
            throw conflict();
        }
        Map<UUID, FileEntity> filesById = new HashMap<>();
        for (FileEntity file : fileRepository.findAllById(storedAttachmentIds)) {
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
        List<FileResponse> fileResponses = new ArrayList<>(attachedFiles.size());
        for (FileEntity file : attachedFiles) {
            FileResponse response = new FileResponse(
                    file.getId(),
                    file.getName(),
                    file.getContentType(),
                    file.getSize(),
                    "/api/files/" + file.getId());
            fileResponses.add(response);
        }
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
