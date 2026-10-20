package com.chat.modules.auth.service;

import java.util.Optional;
import java.util.UUID;

public interface SessionService {
    /** Call only after authentication has verified the user's credentials. */
    IssuedSession create(UUID userId);

    Optional<SessionIdentity> findActive(String rawToken);

    /** Idempotent: missing, expired or already revoked sessions need no special handling. */
    void revoke(String rawToken);
}
