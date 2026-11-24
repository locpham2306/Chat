package com.chat.modules.message.dto.response;

import com.chat.modules.file.dto.response.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Chứa toàn bộ dữ liệu một tin nhắn để BE trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID id;
    private UUID conversationId;
    private MessageSenderResponse sender;
    private String clientMessageId;
    private String content;
    private List<FileResponse> attachments;
    private Instant createdAt;
}
