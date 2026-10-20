package com.chat.modules.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/** Internal service result; controllers must never use this as a response DTO. */
@Getter
public final class IssuedSession {
    private final UUID sessionId;
    private final Instant expiresAt;
    @JsonIgnore
    private final String rawToken;

    public IssuedSession(UUID sessionId, Instant expiresAt, String rawToken) {
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
        this.rawToken = rawToken;
    }
}
