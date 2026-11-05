package com.chat.modules.websocket.dto;

import java.util.UUID;

public record TypingUserData(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
