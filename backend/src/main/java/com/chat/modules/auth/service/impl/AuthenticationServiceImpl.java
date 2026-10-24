package com.chat.modules.auth.service.impl;

import com.chat.exception.BadRequestException;
import com.chat.exception.BusinessException;
import com.chat.exception.DataExistedException;
import com.chat.exception.UnauthenticatedException;
import com.chat.modules.auth.constant.AuthMessageConstants;
import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.service.AuthenticationService;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private final UserRepository users;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validatePasswordLength(request.password());
        var username = StringUtils.trim(request.username());
        var fullName = StringUtils.trim(request.fullName());
        if (users.existsByUsername(username)) {
            throw usernameExists();
        }
        try {
            var user = users.saveAndFlush(
                    new UserEntity(username, passwordEncoder.encode(request.password()), fullName));
            return userMapper.toResponse(user, UserStatusEnum.OFFLINE);
        } catch (DataIntegrityViolationException exception) {
            // The database unique key is authoritative under concurrent registration.
            throw usernameExists();
        }
    }

    @Override
    @Transactional
    public IssuedSession login(LoginRequest request) {
        validatePasswordLength(request.password());
        var user = users.findByUsername(StringUtils.trim(request.username()))
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return sessions.create(user.getId());
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BadRequestException(AuthMessageConstants.PASSWORD_TOO_LONG);
        }
    }

    private BusinessException invalidCredentials() {
        return new UnauthenticatedException(AuthMessageConstants.INVALID_CREDENTIALS);
    }

    private BusinessException usernameExists() {
        return new DataExistedException(AuthMessageConstants.USERNAME_EXISTS);
    }
}
