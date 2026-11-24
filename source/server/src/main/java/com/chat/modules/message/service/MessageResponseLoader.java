package com.chat.modules.message.service;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageAttachmentEntity;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.mapper.MessageMapper;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageResponseLoader {
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public Map<UUID, MessageResponse> load(Collection<MessageEntity> messages) {
        if (messages.isEmpty()) {
            return Map.of();
        }

        List<UUID> messageIds = new ArrayList<>(messages.size());
        LinkedHashSet<UUID> senderIds = new LinkedHashSet<>();
        for (MessageEntity message : messages) {
            messageIds.add(message.getId());
            senderIds.add(message.getSenderId());
        }

        Map<UUID, UserEntity> sendersById = new HashMap<>();
        for (UserEntity sender : userRepository.findAllById(senderIds)) {
            sendersById.put(sender.getId(), sender);
        }

        List<MessageAttachmentEntity> attachmentLinks =
                messageAttachmentRepository.findByMessageIdInOrderByMessageIdAscPositionAsc(messageIds);

        LinkedHashSet<UUID> fileIds = new LinkedHashSet<>();
        for (MessageAttachmentEntity link : attachmentLinks) {
            fileIds.add(link.getFileId());
        }

        Map<UUID, FileEntity> filesById = new HashMap<>();
        if (!fileIds.isEmpty()) {
            for (FileEntity file : fileRepository.findAllById(fileIds)) {
                filesById.put(file.getId(), file);
            }
        }

        Map<UUID, List<FileEntity>> filesByMessageId = new HashMap<>();
        for (MessageAttachmentEntity link : attachmentLinks) {
            FileEntity file = filesById.get(link.getFileId());
            if (file != null) {
                List<FileEntity> messageFiles = filesByMessageId.get(link.getMessageId());
                if (messageFiles == null) {
                    messageFiles = new ArrayList<>();
                    filesByMessageId.put(link.getMessageId(), messageFiles);
                }
                messageFiles.add(file);
            }
        }

        Map<UUID, MessageResponse> responsesById = new HashMap<>();
        for (MessageEntity message : messages) {
            UserEntity sender = sendersById.get(message.getSenderId());
            if (sender == null) {
                throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
            }
            MessageResponse response = messageMapper.toResponse(
                    message, sender, filesByMessageId.getOrDefault(message.getId(), List.of()));
            responsesById.put(message.getId(), response);
        }
        return responsesById;
    }
}
