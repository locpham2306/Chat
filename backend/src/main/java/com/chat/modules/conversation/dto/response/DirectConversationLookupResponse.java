package com.chat.modules.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Trả cuộc hội thoại DIRECT nếu đã tồn tại và thông tin người nhận cho FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectConversationLookupResponse {
    private DirectConversationResponse conversation;
    private ConversationPeerResponse peer;
}
