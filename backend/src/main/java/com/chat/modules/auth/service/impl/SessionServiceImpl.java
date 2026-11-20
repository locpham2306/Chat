package com.chat.modules.auth.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.modules.auth.entity.SessionEntity;
import com.chat.modules.auth.repository.SessionRepository;
import com.chat.modules.auth.security.SessionTokenGenerator;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private static final Duration LIFETIME = Duration.ofDays(365);
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionTokenGenerator sessionTokenGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public IssuedSession create(UUID userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        String rawToken = sessionTokenGenerator.generate();
        SessionEntity session = sessionRepository.saveAndFlush(
                new SessionEntity(userId, sessionTokenGenerator.hash(rawToken), now, now.plus(LIFETIME)));
        return new IssuedSession(session.getId(), session.getExpiresAt(), rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionIdentity> findActive(String rawToken) {
        if (!sessionTokenGenerator.isValidFormat(rawToken)) {
            return Optional.empty();
        }
        return sessionRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                        sessionTokenGenerator.hash(rawToken), clock.instant())
                .map(session -> new SessionIdentity(session.getId(), session.getUserId(), session.getExpiresAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId) {
        return sessionId != null
                && sessionRepository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, clock.instant());
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (sessionTokenGenerator.isValidFormat(rawToken)) {
            sessionRepository.revokeByTokenHash(
                    sessionTokenGenerator.hash(rawToken), clock.instant().truncatedTo(ChronoUnit.MICROS));
        }
    }
}
