package com.chat.modules.user.service;

import com.chat.modules.user.enums.UserStatusEnum;

import java.util.UUID;

public interface UserPresence {
    UserStatusEnum status(UUID userId);
}
