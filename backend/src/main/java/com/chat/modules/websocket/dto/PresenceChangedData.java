package com.chat.modules.websocket.dto;

import com.chat.modules.user.enums.UserStatusEnum;

import java.time.Instant;
import java.util.UUID;

public record PresenceChangedData(
        UUID userId,
        UserStatusEnum status,
        Instant lastSeen) {
}
