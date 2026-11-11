package com.chat.modules.websocket.service;

import com.chat.modules.websocket.dto.TypingData;

import java.util.List;
import java.util.UUID;

public record TypingChange(TypingData data, List<UUID> audienceUserIds) {
}
