package com.chat.modules.conversation.dto.response;

public record DirectConversationLookupResponse(
        DirectConversationResponse conversation,
        ConversationPeerResponse peer) {
}
