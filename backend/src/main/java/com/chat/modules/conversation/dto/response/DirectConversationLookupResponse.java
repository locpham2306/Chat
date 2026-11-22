package com.chat.modules.conversation.dto.response;

// Trả cuộc hội thoại DIRECT nếu đã tồn tại và thông tin người nhận cho FE.
public record DirectConversationLookupResponse(
        DirectConversationResponse conversation,
        ConversationPeerResponse peer) {
}
