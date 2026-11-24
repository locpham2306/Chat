package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin nhóm vừa được tạo để BE trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupConversationResponse {
    private UUID id;
    private ConversationTypeEnum type;
    private String name;
    private UUID avatarFileId;
    private UUID ownerId;
    private int memberCount;
    private Instant createdAt;
}
