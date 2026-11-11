package com.chat.modules.auth;

import com.chat.exception.BusinessException;
import com.chat.modules.auth.constant.AuthMessageConstants;
import com.chat.modules.auth.dto.request.LoginRequest;
import com.chat.modules.auth.dto.request.RegisterRequest;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.auth.service.impl.AuthenticationServiceImpl;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final SessionService sessions = mock(SessionService.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private final AuthenticationServiceImpl service = new AuthenticationServiceImpl(
            users, new UserMapper(), passwords, sessions);

    @Test
    void registerTrimsProfileEncodesPasswordAndReturnsPublicUser() {
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            var user = invocation.<UserEntity>getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        var response = service.register(new RegisterRequest("  an  ", "MatKhauDemo123", "  Nguyễn Văn An  "));

        var capture = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        verify(users).existsByUsername("an");
        verify(users).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getUsername()).isEqualTo("an");
        assertThat(capture.getValue().getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(capture.getValue().getPasswordHash()).doesNotContain("MatKhauDemo123");
        assertThat(passwords.matches("MatKhauDemo123", capture.getValue().getPasswordHash())).isTrue();
        assertThat(response.status()).isEqualTo(UserStatusEnum.OFFLINE);
        assertThat(response.avatarFileId()).isNull();
    }

    @Test
    void registerRejectsExistingUsernameAndDatabaseRaceAsConflict() {
        when(users.existsByUsername("an")).thenReturn(true);
        assertBusinessException(
                () -> service.register(new RegisterRequest("an", "password", "An")),
                HttpStatus.CONFLICT, AuthMessageConstants.USERNAME_EXISTS);
        reset(users);
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertBusinessException(
                () -> service.register(new RegisterRequest("an", "password", "An")),
                HttpStatus.CONFLICT, AuthMessageConstants.USERNAME_EXISTS);
    }

    @Test
    void bcryptByteLimitIsAppliedToRegisterAndLogin() {
        var password = "á".repeat(37);
        assertBusinessException(
                () -> service.register(new RegisterRequest("an", password, "An")),
                HttpStatus.BAD_REQUEST, AuthMessageConstants.PASSWORD_TOO_LONG);
        assertBusinessException(
                () -> service.login(new LoginRequest("an", password)),
                HttpStatus.BAD_REQUEST, AuthMessageConstants.PASSWORD_TOO_LONG);
        verifyNoInteractions(users, sessions);
    }

    @Test
    void loginUsesSameGenericErrorForUnknownUsernameAndWrongPassword() {
        assertBusinessException(
                () -> service.login(new LoginRequest("unknown", "password")),
                HttpStatus.UNAUTHORIZED, AuthMessageConstants.INVALID_CREDENTIALS);
        var user = new UserEntity("an", passwords.encode("correct"), "An");
        when(users.findByUsername("an")).thenReturn(Optional.of(user));
        assertBusinessException(
                () -> service.login(new LoginRequest(" an ", "wrong")),
                HttpStatus.UNAUTHORIZED, AuthMessageConstants.INVALID_CREDENTIALS);
        verifyNoInteractions(sessions);
    }

    @Test
    void validLoginCreatesSessionForResolvedUser() {
        var userId = UUID.randomUUID();
        var user = new UserEntity("an", passwords.encode("correct"), "An");
        ReflectionTestUtils.setField(user, "id", userId);
        var issued = new IssuedSession(UUID.randomUUID(), Instant.now().plusSeconds(60), "A".repeat(43));
        when(users.findByUsername("an")).thenReturn(Optional.of(user));
        when(sessions.create(userId)).thenReturn(issued);

        var result = service.login(new LoginRequest("an", "correct"));
        assertThat(result).isSameAs(issued);
        verify(sessions).create(userId);
    }

    @Test
    void credentialRequestsRedactPasswordFromToString() {
        var password = "secret-value";
        assertThat(new LoginRequest("an", password).toString()).doesNotContain(password).contains("<redacted>");
        assertThat(new RegisterRequest("an", password, "An").toString())
                .doesNotContain(password).contains("<redacted>");
    }

    private void assertBusinessException(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            HttpStatus status,
            String messageKey) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(status);
            assertThat(exception.getMessageKey()).isEqualTo(messageKey);
        });
    }
}
