package com.chat;

import com.chat.modules.auth.entity.SessionEntity;
import com.chat.modules.auth.repository.SessionRepository;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration")
class ChatApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("chat_test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UserRepository users;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private EntityManager entityManager;

    @Test
    void healthEndpointReportsUpWithoutExposingDetails() {
        var response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void environmentEndpointIsNotExposed() {
        var response = restTemplate.getForEntity("/actuator/env", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void migrationsAreAppliedAndNotRepeated() {
        assertThat(flyway.info().applied()).hasSize(3);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void unicodeRoundTripsAndTransactionsRollBack() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO persistence_probe (id, content) VALUES (?, ?)", 1L, "Xin chào 👋");
            assertThat(jdbc.queryForObject(
                    "SELECT content FROM persistence_probe WHERE id = ?", String.class, 1L))
                    .isEqualTo("Xin chào 👋");
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persistence_probe", Long.class)).isZero();
    }

    @Test
    void userMappingPersistsUuidUnicodeAndAuditFields() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("MappingUser", "encoded-password-fixture", "Nguyễn An 👋"));
            var id = user.getId();
            entityManager.clear();
            var loaded = users.findByUsername("mappinguser").orElseThrow();
            assertThat(loaded.getId()).isEqualTo(id);
            assertThat(loaded.getFullName()).isEqualTo("Nguyễn An 👋");
            assertThat(loaded.getPasswordHash()).isEqualTo("encoded-password-fixture");
            assertThat(loaded.getCreatedAt()).isNotNull();
            assertThat(loaded.getUpdatedAt()).isNotNull();
            assertThat(loaded.getAvatarFileId()).isNull();
            assertThat(loaded.getLastSeen()).isNull();
            status.setRollbackOnly();
        });
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateUsernames() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            users.saveAndFlush(new UserEntity("UniqueUser", "hash-fixture", "An"));
            users.saveAndFlush(new UserEntity("uniqueuser", "hash-fixture", "Bình"));
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(users.existsByUsername("UniqueUser")).isFalse();
    }

    @Test
    void auditingUpdatesModifiedTimeAndPreservesCreatedTime() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("AuditUser", "hash-fixture", "An"));
            var id = user.getId();
            entityManager.clear();
            var loaded = users.findById(id).orElseThrow();
            var created = loaded.getCreatedAt();
            var updated = loaded.getUpdatedAt();
            // Exercise dirty checking without adding a production profile-update API to this step.
            ReflectionTestUtils.setField(loaded, "fullName", "Bình");
            users.flush();
            entityManager.clear();
            var changed = users.findById(id).orElseThrow();
            assertThat(changed.getFullName()).isEqualTo("Bình");
            assertThat(changed.getCreatedAt()).isEqualTo(created);
            assertThat(changed.getUpdatedAt()).isAfter(updated);
            status.setRollbackOnly();
        });
    }

    @Test
    void sessionExpiresAtExactBoundaryAndRevocationIsIdempotent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("SessionUser", "hash-fixture", "An"));
            var now = Instant.parse("2026-10-20T00:00:00Z");
            var expiry = now.plus(365, ChronoUnit.DAYS);
            var hash = "a".repeat(64);
            var session = sessions.saveAndFlush(new SessionEntity(user.getId(), hash, now, expiry));
            var id = session.getId();
            entityManager.clear();
            var loaded = sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, now).orElseThrow();
            assertThat(loaded.getId()).isEqualTo(id);
            assertThat(loaded.getUserId()).isEqualTo(user.getId());
            assertThat(loaded.getExpiresAt()).isEqualTo(expiry);
            assertThat(sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, expiry)).isEmpty();
            assertThat(sessions.revokeByTokenHash(hash, now.plusSeconds(1))).isEqualTo(1);
            assertThat(sessions.revokeByTokenHash(hash, now.plusSeconds(2))).isZero();
            assertThat(sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, now)).isEmpty();
            assertThat(sessions.findById(id).orElseThrow().getRevokedAt()).isEqualTo(now.plusSeconds(1));
            assertThat(sessions.revokeByTokenHash("f".repeat(64), now)).isZero();
            status.setRollbackOnly();
        });
    }

    @Test
    void databaseRejectsSessionForMissingUser() {
        var now = Instant.parse("2026-10-20T00:00:00Z");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                sessions.saveAndFlush(new SessionEntity(UUID.randomUUID(), "b".repeat(64),
                        now, now.plusSeconds(60)))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateTokenHash() {
        var now = Instant.parse("2026-10-20T00:00:00Z");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("TokenUser", "hash-fixture", "An"));
            sessions.saveAndFlush(new SessionEntity(user.getId(), "c".repeat(64), now, now.plusSeconds(60)));
            sessions.saveAndFlush(new SessionEntity(user.getId(), "c".repeat(64), now, now.plusSeconds(60)));
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sessionRejectsRawTokenAndInvalidExpiry() {
        var now = Instant.parse("2026-10-20T00:00:00Z");
        assertThatThrownBy(() -> new SessionEntity(UUID.randomUUID(), "raw-token", now, now.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionEntity(UUID.randomUUID(), "d".repeat(64), now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
