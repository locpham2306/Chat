package com.chat.modules.user.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.dto.response.PageResponse;
import com.chat.exception.BusinessException;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserPresence;
import com.chat.modules.user.service.UserService;
import com.chat.utils.PageableUtils;
import com.chat.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final UserMapper userMapper;
    private final UserPresence userPresence;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            FileRepository fileRepository,
            UserMapper userMapper,
            UserPresence userPresence) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.userMapper = userMapper;
        this.userPresence = userPresence;
    }

    public UserServiceImpl(
            UserRepository userRepository,
            FileRepository fileRepository,
            UserMapper userMapper) {
        this(userRepository, fileRepository, userMapper, ignored -> UserStatusEnum.OFFLINE);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        UserEntity user = currentUser(userId);
        UserStatusEnum status = userPresence.status(user.getId());
        return userMapper.toResponse(user, status);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UUID userId, UpdateProfileRequest request) {
        UserEntity user = this.currentUser(userId);
        if (request.avatarFileId() != null) {
            FileEntity avatar = fileRepository.findById(request.avatarFileId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
            boolean allowed = userId.equals(avatar.getUploaderId()) // File do chính user upload
                    && avatar.getConversationId() == null // File không thuộc hội thoại
                    && StringUtils.isImageContentType(avatar.getContentType());  // File phải là ảnh
            if (!allowed) {
                throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
            }
        }
        user.updateProfile(StringUtils.trim(request.fullName()), request.avatarFileId());
        userRepository.save(user);
        UserStatusEnum status = userPresence.status(user.getId());
        return userMapper.toResponse(user, status);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<UserResponse>> searchUsers(
            UUID currentUserId, String keyword, Integer page, Integer size) {
        String resolvedKeyword = StringUtils.trim(keyword);
        if (resolvedKeyword == null) {
            resolvedKeyword = "";
        }
        Sort sort = Sort.by(Sort.Order.asc("username"), Sort.Order.asc("id"));

        Pageable pageable = PageableUtils.of(page, size, sort);

        Page<UserEntity> usersPage = userRepository.searchPublicProfiles(currentUserId, resolvedKeyword, pageable);

        List<UserResponse> userResponses = new ArrayList<>();

        for (UserEntity user : usersPage.getContent()) {
            UserStatusEnum status = userPresence.status(user.getId());
            UserResponse userResponse = userMapper.toResponse(user, status);
            userResponses.add(userResponse);
        }

        return PageResponse.of(userResponses, usersPage.getTotalPages(), usersPage.getTotalElements());
    }

    private UserEntity currentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthenticated(ErrorConstants.UNAUTHENTICATED));
    }
}
