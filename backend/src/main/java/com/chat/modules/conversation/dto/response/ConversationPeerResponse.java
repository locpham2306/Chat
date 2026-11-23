package com.chat.modules.conversation.dto.response;

import com.chat.modules.user.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Chứa thông tin người đang chat cùng user hiện tại trong hội thoại DIRECT.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationPeerResponse {
    private UUID id;
    private String username;
    private String fullName;
    private UUID avatarFileId;
    private UserStatusEnum status;
}
