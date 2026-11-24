package com.chat.modules.user.mapper;

import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(UserEntity user, UserStatusEnum status) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getAvatarFileId(), status, user.getLastSeen());
    }
}
