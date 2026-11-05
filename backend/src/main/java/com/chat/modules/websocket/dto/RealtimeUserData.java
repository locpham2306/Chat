package com.chat.modules.websocket.dto;

import java.util.UUID;

public record RealtimeUserData(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
