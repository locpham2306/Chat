package com.chat.modules.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

// Cho FE biết user nào vừa được thêm và user nào đã có sẵn trong nhóm.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddConversationMembersResponse {
    private List<UUID> addedUserIds;
    private List<UUID> existingUserIds;
}
