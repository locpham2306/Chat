package com.chat;

import com.chat.modules.auth.entity.SessionEntity;
import com.chat.modules.auth.repository.SessionRepository;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.auth.security.SessionTokenGenerator;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationReadStateEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.entity.MessageAttachmentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration",
        "app.session.cookie.secure=true",
        "app.file.storage-root=target/test-uploads"
})
class ChatApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("chat_test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--log-bin-trust-function-creators=1");

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
    private ConversationRepository conversations;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionTokenGenerator sessionTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

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
    void corsAllowsConfiguredFrontendAndRejectsOtherOrigins() {
        var preflightHeaders = new HttpHeaders();
        preflightHeaders.setOrigin("http://localhost:3001");
        preflightHeaders.set("Access-Control-Request-Method", "PUT");
        preflightHeaders.set("Access-Control-Request-Headers", "content-type");
        var preflight = restTemplate.exchange(
                "/api/users/me", HttpMethod.OPTIONS, new HttpEntity<>(preflightHeaders), String.class);

        assertThat(preflight.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preflight.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3001");
        assertThat(preflight.getHeaders().getAccessControlAllowCredentials()).isTrue();
        assertThat(preflight.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.PUT);

        var actualHeaders = new HttpHeaders();
        actualHeaders.setOrigin("http://localhost:3001");
        var unauthenticated = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(actualHeaders), String.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3001");
        assertThat(unauthenticated.getHeaders().getAccessControlAllowCredentials()).isTrue();

        preflightHeaders.setOrigin("http://malicious.example");
        var rejected = restTemplate.exchange(
                "/api/users/me", HttpMethod.OPTIONS, new HttpEntity<>(preflightHeaders), String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rejected.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void registerLoginCurrentUserAndLogoutFollowCookieSessionContract() {
        var username = "ApiUser";
        var password = "MatKhauDemo123";
        var register = restTemplate.postForEntity("/api/auth/register",
                Map.of("username", username, "password", password, "fullName", "Nguyễn Văn An"),
                String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody())
                .contains("\"username\":\"ApiUser\"", "\"fullName\":\"Nguyễn Văn An\"", "\"status\":\"OFFLINE\"")
                .doesNotContain(password, "passwordHash", "token");
        var user = users.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();

        var login = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", username, "password", password), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody())
                .contains("\"code\":\"0\"", "\"strCode\":\"success\"",
                        "\"content\":{\"message\":\"Đăng nhập thành công\"}");
        var setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("token=", "Max-Age=31536000", "Path=/", "HttpOnly", "Secure", "SameSite=Lax");
        var rawToken = setCookie.substring("token=".length(), setCookie.indexOf(';'));
        assertThat(login.getBody()).doesNotContain(rawToken);
        var stored = sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                sessionTokens.hash(rawToken), Instant.now()).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(user.getId());

        var unauthenticated = restTemplate.getForEntity("/api/users/me", String.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getBody()).contains("\"strCode\":\"UNAUTHENTICATED\"");

        var invalidHeaders = new HttpHeaders();
        invalidHeaders.set(HttpHeaders.COOKIE, "token=invalid");
        var invalidSession = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(invalidHeaders), String.class);
        assertThat(invalidSession.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var sessionHeaders = new HttpHeaders();
        sessionHeaders.set(HttpHeaders.COOKIE, "token=" + rawToken);
        var currentUser = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(sessionHeaders), String.class);
        assertThat(currentUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentUser.getBody())
                .contains("\"id\":\"" + user.getId() + "\"", "\"username\":\"ApiUser\"",
                        "\"fullName\":\"Nguyễn Văn An\"", "\"status\":\"OFFLINE\"")
                .doesNotContain(password, "passwordHash", "token");

        var missingAvatarKeyHeaders = new HttpHeaders();
        missingAvatarKeyHeaders.putAll(sessionHeaders);
        missingAvatarKeyHeaders.setContentType(MediaType.APPLICATION_JSON);
        var missingAvatarKey = restTemplate.exchange("/api/users/me", HttpMethod.PUT,
                new HttpEntity<>(Map.of("fullName", "Tên mới"), missingAvatarKeyHeaders), String.class);
        assertThat(missingAvatarKey.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var updatedProfile = restTemplate.exchange("/api/users/me", HttpMethod.PUT,
                new HttpEntity<>(new UpdateProfileRequest("  Nguyễn Văn Bình  ", null), sessionHeaders),
                String.class);
        assertThat(updatedProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedProfile.getBody())
                .contains("\"fullName\":\"Nguyễn Văn Bình\"", "\"avatarFileId\":null")
                .doesNotContain(password, "passwordHash", "token");
        assertThat(users.findById(user.getId()).orElseThrow().getFullName()).isEqualTo("Nguyễn Văn Bình");

        var logout = restTemplate.exchange("/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(sessionHeaders), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logout.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("token=;", "Max-Age=0", "Path=/", "HttpOnly", "Secure", "SameSite=Lax");
        assertThat(sessionService.findActive(rawToken)).isEmpty();
        var revokedSession = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(sessionHeaders), String.class);
        assertThat(revokedSession.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void searchUsersMatchesPublicProfilesExcludesCurrentUserAndValidatesPagination() {
        var current = users.saveAndFlush(
                new UserEntity("FindPersonX7Current", "hash-fixture", "FindPersonX7 Current"));
        var usernameMatch = users.saveAndFlush(
                new UserEntity("FindPersonX7Alpha", "hash-fixture", "Alpha"));
        var fullNameMatch = users.saveAndFlush(
                new UserEntity("ZedSearchCandidate", "hash-fixture", "findpersonx7 Beta"));
        var unrelated = users.saveAndFlush(
                new UserEntity("UnrelatedCandidate", "hash-fixture", "Unrelated"));
        var issuedSession = sessionService.create(current.getId());
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, "token=" + issuedSession.getRawToken());

        var firstPage = restTemplate.exchange(
                "/api/users?keyword=findpersonx7&page=0&size=1",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstPage.getBody())
                .contains("\"code\":\"0\"", "\"strCode\":\"success\"",
                        "\"id\":\"" + usernameMatch.getId() + "\"",
                        "\"status\":\"OFFLINE\"", "\"totalPages\":2", "\"totalElements\":2")
                .doesNotContain(current.getId().toString(), fullNameMatch.getId().toString(),
                        unrelated.getId().toString(), "passwordHash");

        var secondPage = restTemplate.exchange(
                "/api/users?keyword=FINDPERSONX7&page=1&size=1",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(secondPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondPage.getBody())
                .contains("\"id\":\"" + fullNameMatch.getId() + "\"")
                .doesNotContain(current.getId().toString(), usernameMatch.getId().toString());

        var invalidPage = restTemplate.exchange(
                "/api/users?page=-1&size=101",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidPage.getBody()).contains("\"strCode\":\"VALIDATION_ERROR\"");
    }

    @Test
    void uploadAndDownloadFileEnforceOwnerAndAvatarPermissions() throws Exception {
        byte[] content = "Xin chào từ file".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var owner = users.saveAndFlush(new UserEntity("FileApiOwner", "hash-fixture", "File Owner"));
        var viewer = users.saveAndFlush(new UserEntity("FileApiViewer", "hash-fixture", "File Viewer"));
        var ownerSession = sessionService.create(owner.getId());
        var viewerSession = sessionService.create(viewer.getId());

        var uploadHeaders = sessionHeaders(ownerSession.getRawToken());
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        var multipart = new LinkedMultiValueMap<String, Object>();
        var filePart = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "avatar.png";
            }
        };
        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.IMAGE_PNG);
        multipart.add("file", new HttpEntity<>(filePart, fileHeaders));

        var upload = restTemplate.exchange("/api/files", HttpMethod.POST,
                new HttpEntity<>(multipart, uploadHeaders), String.class);

        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var uploadJson = objectMapper.readTree(upload.getBody());
        var fileId = UUID.fromString(uploadJson.path("content").path("id").asText());
        assertThat(uploadJson.path("content").path("name").asText()).isEqualTo("avatar.png");
        assertThat(uploadJson.path("content").path("contentType").asText()).isEqualTo("image/png");
        assertThat(uploadJson.path("content").path("size").asLong()).isEqualTo(content.length);
        assertThat(uploadJson.path("content").path("downloadUrl").asText())
                .isEqualTo("/api/files/" + fileId);
        var metadata = entityManager.find(FileEntity.class, fileId);
        assertThat(metadata.getStorageKey()).doesNotContain("avatar.png");

        var ownerDownload = restTemplate.exchange("/api/files/" + fileId, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(ownerSession.getRawToken())), byte[].class);
        assertThat(ownerDownload.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownerDownload.getBody()).isEqualTo(content);
        assertThat(ownerDownload.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(ownerDownload.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment", "avatar.png");
        assertThat(ownerDownload.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");

        var forbidden = restTemplate.exchange("/api/files/" + fileId, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(viewerSession.getRawToken())), byte[].class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var profileHeaders = sessionHeaders(ownerSession.getRawToken());
        profileHeaders.setContentType(MediaType.APPLICATION_JSON);
        var updateProfile = restTemplate.exchange("/api/users/me", HttpMethod.PUT,
                new HttpEntity<>(new UpdateProfileRequest("File Owner", fileId), profileHeaders), String.class);
        assertThat(updateProfile.getStatusCode()).isEqualTo(HttpStatus.OK);

        var avatarDownload = restTemplate.exchange("/api/files/" + fileId, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(viewerSession.getRawToken())), byte[].class);
        assertThat(avatarDownload.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(avatarDownload.getBody()).isEqualTo(content);
    }

    @Test
    void directConversationLookupNeverCreatesAnEmptyConversation() throws Exception {
        var current = users.saveAndFlush(
                new UserEntity("DirectLookupCurrent", "hash-fixture", "Direct Current"));
        var recipient = users.saveAndFlush(
                new UserEntity("DirectLookupRecipient", "hash-fixture", "Direct Recipient"));
        var issuedSession = sessionService.create(current.getId());
        var headers = sessionHeaders(issuedSession.getRawToken());
        long conversationCount = conversations.count();

        var missing = restTemplate.exchange(
                "/api/conversations/direct?recipientId=" + recipient.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(missing.getBody()).isNull();
        assertThat(conversations.count()).isEqualTo(conversationCount);

        var conversation = conversations.saveAndFlush(
                ConversationEntity.direct(recipient.getId(), current.getId()));
        var found = restTemplate.exchange(
                "/api/conversations/direct?recipientId=" + recipient.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = objectMapper.readTree(found.getBody()).path("content");
        assertThat(content.path("id").asText()).isEqualTo(conversation.getId().toString());
        assertThat(content.path("type").asText()).isEqualTo("DIRECT");
        assertThat(content.path("memberIds")).hasSize(2);
        assertThat(content.path("memberIds").toString())
                .contains(current.getId().toString(), recipient.getId().toString());
        assertThat(content.path("createdAt").asText()).isNotBlank();

        var self = restTemplate.exchange(
                "/api/conversations/direct?recipientId=" + current.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var unknown = restTemplate.exchange(
                "/api/conversations/direct?recipientId=" + UUID.randomUUID(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var invalid = restTemplate.exchange(
                "/api/conversations/direct?recipientId=not-a-uuid",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var omitted = restTemplate.exchange(
                "/api/conversations/direct",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(omitted.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void migrationsAreAppliedAndNotRepeated() {
        assertThat(flyway.info().applied()).hasSize(5);
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

    @Test
    void sessionServicePersistsResolvesAndRevokesWithoutAffectingOtherSessions() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("ServiceUser", "hash-fixture", "An"));
            var first = sessionService.create(user.getId());
            var second = sessionService.create(user.getId());
            entityManager.clear();
            var stored = sessions.findById(first.getSessionId()).orElseThrow();
            assertThat(stored.getTokenHash()).isEqualTo(sessionTokens.hash(first.getRawToken()));
            assertThat(stored.getExpiresAt()).isEqualTo(stored.getCreatedAt().plus(365, ChronoUnit.DAYS));
            assertThat(sessionService.findActive(first.getRawToken()).orElseThrow().userId()).isEqualTo(user.getId());
            sessionService.revoke(first.getRawToken());
            var revokedAt = sessions.findById(first.getSessionId()).orElseThrow().getRevokedAt();
            sessionService.revoke(first.getRawToken());
            assertThat(sessions.findById(first.getSessionId()).orElseThrow().getRevokedAt()).isEqualTo(revokedAt);
            assertThat(sessionService.findActive(first.getRawToken())).isEmpty();
            assertThat(sessionService.findActive(second.getRawToken())).isPresent();
            status.setRollbackOnly();
        });
    }

    @Test
    void chatEntitiesRoundTripWithAttachmentsAndReadMarker() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("DomainUser", "hash", "An"));
            var conversation = persist(ConversationEntity.group(user.getId(), "Nhóm 👋", null));
            var member = persist(new ConversationMemberEntity(conversation.getId(), user.getId(),
                    MemberRoleEnum.OWNER, Instant.now()));
            var message = persist(new MessageEntity(conversation.getId(), user.getId(), "client-1", "Xin chào"));
            var file = new FileEntity(user.getId(), "ảnh.png", "test/image.png", "image/png", 12L);
            file.bindToConversation(conversation.getId());
            persist(file);
            var attachment = persist(new MessageAttachmentEntity(message.getId(), file.getId(), 0));
            persist(new ConversationReadStateEntity(member.getId(), message.getId()));
            entityManager.clear();
            assertThat(entityManager.find(ConversationEntity.class, conversation.getId()).getName()).isEqualTo("Nhóm 👋");
            assertThat(entityManager.find(ConversationReadStateEntity.class, member.getId()).getLastReadMessageId())
                    .isEqualTo(message.getId());
            assertThat(entityManager.find(MessageEntity.class, message.getId()).getContent()).isEqualTo("Xin chào");
            assertThat(entityManager.find(FileEntity.class, file.getId()).getSize()).isEqualTo(12L);
            assertThat(entityManager.find(MessageAttachmentEntity.class, attachment.getId()).getFileId()).isEqualTo(file.getId());
            status.setRollbackOnly();
        });
    }

    @Test
    void directPairCannotBeDuplicatedInReverseOrder() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var a = users.saveAndFlush(new UserEntity("PairA", "hash", "A"));
            var b = users.saveAndFlush(new UserEntity("PairB", "hash", "B"));
            persist(ConversationEntity.direct(a.getId(), b.getId()));
            persist(ConversationEntity.direct(b.getId(), a.getId()));
        })).hasRootCauseInstanceOf(java.sql.SQLException.class);
    }

    @Test
    void senderClientMessageIdCannotBeDuplicatedAcrossConversations() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("RetryUser", "hash", "A"));
            var first = persist(ConversationEntity.group(user.getId(), "First", null));
            var second = persist(ConversationEntity.group(user.getId(), "Second", null));
            persist(new MessageEntity(first.getId(), user.getId(), "same-client-id", "Hello"));
            persist(new MessageEntity(second.getId(), user.getId(), "same-client-id", "Hello"));
        })).hasRootCauseInstanceOf(java.sql.SQLException.class);
    }

    @Test
    void readMarkerCannotReferenceAnotherConversation() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("ReadUser", "hash", "A"));
            var first = persist(ConversationEntity.group(user.getId(), "First", null));
            var second = persist(ConversationEntity.group(user.getId(), "Second", null));
            var member = persist(new ConversationMemberEntity(first.getId(), user.getId(), MemberRoleEnum.OWNER, Instant.now()));
            var message = persist(new MessageEntity(second.getId(), user.getId(), "read-1", "Hello"));
            persist(new ConversationReadStateEntity(member.getId(), message.getId()));
        })).hasRootCauseInstanceOf(java.sql.SQLException.class);
    }

    @Test
    void attachmentCannotUseFileBoundToAnotherConversation() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.saveAndFlush(new UserEntity("FileUser", "hash", "A"));
            var first = persist(ConversationEntity.group(user.getId(), "First", null));
            var second = persist(ConversationEntity.group(user.getId(), "Second", null));
            var file = new FileEntity(user.getId(), "file.txt", "test/file.txt", "text/plain", 5);
            file.bindToConversation(first.getId());
            persist(file);
            var message = persist(new MessageEntity(second.getId(), user.getId(), "file-1", null));
            persist(new MessageAttachmentEntity(message.getId(), file.getId(), 0));
        })).hasRootCauseInstanceOf(java.sql.SQLException.class);
    }

    private HttpHeaders sessionHeaders(String rawToken) {
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, "token=" + rawToken);
        return headers;
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
