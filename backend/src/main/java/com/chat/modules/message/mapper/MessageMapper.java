package com.chat.modules.message.mapper;

import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageMapper {

    public MessageResponse toResponse(MessageEntity message, List<FileEntity> attachments) {
        var files = attachments.stream()
                .map(file -> new FileResponse(
                        file.getId(), file.getName(), file.getContentType(), file.getSize(),
                        "/api/files/" + file.getId()))
                .toList();
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getClientMessageId(),
                message.getContent(),
                files,
                message.getCreatedAt());
    }
}
