package com.chat.modules.user.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.exception.BusinessException;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, ErrorConstants.UNAUTHENTICATED));
        return userMapper.toResponse(user, UserStatusEnum.OFFLINE);
    }
}
