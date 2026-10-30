package com.chat.modules.message.dto.response;

import java.util.UUID;

public record MessageSenderResponse(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
