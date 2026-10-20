package com.chat.modules.auth.service;

import java.time.Instant;
import java.util.UUID;

public record SessionIdentity(UUID sessionId, UUID userId, Instant expiresAt) {
}
