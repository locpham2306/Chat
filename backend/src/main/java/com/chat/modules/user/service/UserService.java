package com.chat.modules.user.service;

import com.chat.modules.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUser(UUID userId);
}
