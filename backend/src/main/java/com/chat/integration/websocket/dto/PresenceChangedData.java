package com.chat.integration.websocket.dto;

import com.chat.modules.user.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Báo realtime cho FE rằng một user vừa ONLINE hoặc OFFLINE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresenceChangedData {
    private UUID userId;
    private RealtimeUserData user;
    private UserStatusEnum status;
    private Instant lastSeen;
}
