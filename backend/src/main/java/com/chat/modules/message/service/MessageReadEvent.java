package com.chat.modules.message.service;

import com.chat.modules.message.dto.response.ConversationReadResponse;

import java.util.List;
import java.util.UUID;

public record MessageReadEvent(
        ConversationReadResponse response,
        List<UUID> activeMemberIds) {

    public MessageReadEvent {
        activeMemberIds = List.copyOf(activeMemberIds);
    }
}
