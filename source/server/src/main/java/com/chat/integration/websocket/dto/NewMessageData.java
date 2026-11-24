package com.chat.integration.websocket.dto;

import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.message.dto.response.MessageSenderResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Chứa dữ liệu tin nhắn mới để BE gửi realtime lên FE của các thành viên.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageData {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private MessageSenderResponse sender;
    private String clientMessageId;
    private String content;
    private List<FileResponse> attachments;
    private Instant createdAt;
}
