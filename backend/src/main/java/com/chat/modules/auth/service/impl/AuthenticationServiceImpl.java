package com.chat.modules.auth.service.impl;

import com.chat.exception.BusinessException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validatePasswordLength(request.password());
        String username = StringUtils.trim(request.username());
        String fullName = StringUtils.trim(request.fullName());
        if (userRepository.existsByUsername(username)) {
            throw usernameExists();
        }
        try {
            String passwordHash = passwordEncoder.encode(request.password());
            UserEntity newUser = new UserEntity(username, passwordHash, fullName);
            UserEntity user = userRepository.saveAndFlush(newUser);
            return userMapper.toResponse(user, UserStatusEnum.OFFLINE);
        } catch (DataIntegrityViolationException exception) {
            throw usernameExists();
        }
    }

    @Override
    @Transactional
    public IssuedSession login(LoginRequest request) {
        validatePasswordLength(request.password());
        String username = StringUtils.trim(request.username());
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        UUID userId = user.getId();
        return sessionService.create(userId);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw BusinessException.badRequest(AuthMessageConstants.PASSWORD_TOO_LONG);
        }
    }

    private BusinessException invalidCredentials() {
        return BusinessException.unauthenticated(AuthMessageConstants.INVALID_CREDENTIALS);
    }

    private BusinessException usernameExists() {
        return BusinessException.conflict(AuthMessageConstants.USERNAME_EXISTS);
    }
}
