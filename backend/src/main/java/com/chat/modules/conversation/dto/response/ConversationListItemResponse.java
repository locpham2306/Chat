package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.message.dto.response.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Chứa dữ liệu một dòng hội thoại trong danh sách chat của FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListItemResponse {
    private UUID id;
    private ConversationTypeEnum type;
    private String name;
    private UUID avatarFileId;
    private UUID ownerId;
    private long memberCount;
    private Instant createdAt;
    private ConversationPeerResponse peer;
    private MessageResponse lastMessage;
    private long unreadCount;
}
