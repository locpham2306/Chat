package com.chat.modules.user.service;

import com.chat.common.dto.response.PageResponse;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUser(UUID userId);

    UserResponse updateCurrentUser(UUID userId, UpdateProfileRequest request);

    PageResponse<List<UserResponse>> searchUsers(
            UUID currentUserId, String keyword, Integer page, Integer size);
}
