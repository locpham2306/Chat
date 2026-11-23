package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin cơ bản của một cuộc hội thoại DIRECT để trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectConversationResponse {
    private UUID id;
    private ConversationTypeEnum type;
    private Instant createdAt;
}
