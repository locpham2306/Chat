package com.chat.modules.websocket.service;

import com.chat.modules.websocket.dto.PresenceChangedData;

import java.util.List;
import java.util.UUID;

public record PresenceChange(PresenceChangedData data, List<UUID> audienceUserIds) {
}
