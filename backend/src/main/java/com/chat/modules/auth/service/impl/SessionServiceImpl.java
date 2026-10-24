package com.chat.modules.auth.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.NotFoundException;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private static final Duration LIFETIME = Duration.ofDays(365);
    private final SessionRepository sessions;
    private final UserRepository users;
    private final SessionTokenGenerator tokens;
    private final Clock clock;

    @Override
    @Transactional
    public IssuedSession create(UUID userId) {
        if (userId == null || !users.existsById(userId)) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var rawToken = tokens.generate();
        var session = sessions.saveAndFlush(
                new SessionEntity(userId, tokens.hash(rawToken), now, now.plus(LIFETIME)));
        return new IssuedSession(session.getId(), session.getExpiresAt(), rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionIdentity> findActive(String rawToken) {
        if (!tokens.isValidFormat(rawToken)) {
            return Optional.empty();
        }
        return sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokens.hash(rawToken), clock.instant())
                .map(session -> new SessionIdentity(session.getId(), session.getUserId(), session.getExpiresAt()));
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (tokens.isValidFormat(rawToken)) {
            sessions.revokeByTokenHash(tokens.hash(rawToken), clock.instant().truncatedTo(ChronoUnit.MICROS));
        }
    }
}
