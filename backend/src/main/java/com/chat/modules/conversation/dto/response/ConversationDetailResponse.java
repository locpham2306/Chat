package com.chat.modules.conversation.dto.response;

import com.chat.modules.conversation.enums.ConversationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin chi tiết của một cuộc hội thoại để trả lên màn hình chi tiết của FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {
    private UUID id;
    private ConversationTypeEnum type;
    private String name;
    private UUID avatarFileId;
    private UUID ownerId;
    private long memberCount;
    private Instant createdAt;
    private ConversationPeerResponse peer;
}
