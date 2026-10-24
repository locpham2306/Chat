package com.chat.modules.user.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.NoPermissionException;
import com.chat.exception.NotFoundException;
import com.chat.exception.UnauthenticatedException;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserService;
import com.chat.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final FileRepository files;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        var user = currentUser(userId);
        return userMapper.toResponse(user, UserStatusEnum.OFFLINE);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UUID userId, UpdateProfileRequest request) {
        var user = currentUser(userId);
        if (request.avatarFileId() != null) {
            var avatar = files.findById(request.avatarFileId())
                    .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
            boolean allowed = userId.equals(avatar.getUploaderId())
                    && avatar.getConversationId() == null
                    && StringUtils.isImageContentType(avatar.getContentType());
            if (!allowed) {
                throw new NoPermissionException(ErrorConstants.FORBIDDEN);
            }
        }
        user.updateProfile(StringUtils.trim(request.fullName()), request.avatarFileId());
        return userMapper.toResponse(user, UserStatusEnum.OFFLINE);
    }

    private UserEntity currentUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new UnauthenticatedException(ErrorConstants.UNAUTHENTICATED));
    }
}
