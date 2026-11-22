package com.chat.integration.websocket.dto;

import com.chat.modules.user.enums.UserStatusEnum;

import java.time.Instant;
import java.util.UUID;

// Báo realtime cho FE rằng một user vừa ONLINE hoặc OFFLINE.
public record PresenceChangedData(
        UUID userId,
        RealtimeUserData user,
        UserStatusEnum status,
        Instant lastSeen) {
}
