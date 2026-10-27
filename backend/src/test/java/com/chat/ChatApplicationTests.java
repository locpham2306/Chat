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
import com.chat.modules.conversation.repository.ConversationMemberRepository;
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
    private ConversationMemberRepository conversationMembers;

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

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.OK);
        var missingContent = objectMapper.readTree(missing.getBody()).path("content");
        assertThat(missingContent.path("conversation").isNull()).isTrue();
        assertThat(missingContent.path("peer").path("id").asText())
                .isEqualTo(recipient.getId().toString());
        assertThat(missingContent.path("peer").path("username").asText())
                .isEqualTo("DirectLookupRecipient");
        assertThat(missingContent.path("peer").path("fullName").asText())
                .isEqualTo("Direct Recipient");
        assertThat(missingContent.path("peer").path("status").asText()).isEqualTo("OFFLINE");
        assertThat(conversations.count()).isEqualTo(conversationCount);

        var conversation = conversations.saveAndFlush(
                ConversationEntity.direct(recipient.getId(), current.getId()));
        var found = restTemplate.exchange(
                "/api/conversations/direct?recipientId=" + recipient.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = objectMapper.readTree(found.getBody()).path("content");
        assertThat(content.path("conversation").path("id").asText())
                .isEqualTo(conversation.getId().toString());
        assertThat(content.path("conversation").path("type").asText()).isEqualTo("DIRECT");
        assertThat(content.path("conversation").path("createdAt").asText()).isNotBlank();
        assertThat(content.path("peer").path("id").asText())
                .isEqualTo(recipient.getId().toString());

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
    void createGroupAssignsRolesAndAllowsMembersToReadGroupAvatar() throws Exception {
        byte[] avatarContent = "group-avatar".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var owner = users.saveAndFlush(
                new UserEntity("GroupApiOwner", "hash-fixture", "Group Owner"));
        var member = users.saveAndFlush(
                new UserEntity("GroupApiMember", "hash-fixture", "Group Member"));
        var outsider = users.saveAndFlush(
                new UserEntity("GroupApiOutsider", "hash-fixture", "Group Outsider"));
        var ownerSession = sessionService.create(owner.getId());
        var memberSession = sessionService.create(member.getId());
        var outsiderSession = sessionService.create(outsider.getId());

        var uploadHeaders = sessionHeaders(ownerSession.getRawToken());
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        var multipart = new LinkedMultiValueMap<String, Object>();
        var avatarPart = new ByteArrayResource(avatarContent) {
            @Override
            public String getFilename() {
                return "group.png";
            }
        };
        var avatarHeaders = new HttpHeaders();
        avatarHeaders.setContentType(MediaType.IMAGE_PNG);
        multipart.add("file", new HttpEntity<>(avatarPart, avatarHeaders));
        var upload = restTemplate.exchange("/api/files", HttpMethod.POST,
                new HttpEntity<>(multipart, uploadHeaders), String.class);
        var avatarId = UUID.fromString(objectMapper.readTree(upload.getBody())
                .path("content").path("id").asText());

        var groupHeaders = sessionHeaders(ownerSession.getRawToken());
        groupHeaders.setContentType(MediaType.APPLICATION_JSON);
        var create = restTemplate.exchange("/api/conversations/group", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "  Nhóm BTL  ",
                        "memberIds", java.util.List.of(member.getId()),
                        "avatarFileId", avatarId), groupHeaders),
                String.class);

        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var content = objectMapper.readTree(create.getBody()).path("content");
        var conversationId = UUID.fromString(content.path("id").asText());
        assertThat(content.path("type").asText()).isEqualTo("GROUP");
        assertThat(content.path("name").asText()).isEqualTo("Nhóm BTL");
        assertThat(content.path("avatarFileId").asText()).isEqualTo(avatarId.toString());
        assertThat(content.path("ownerId").asText()).isEqualTo(owner.getId().toString());
        assertThat(content.path("memberCount").asInt()).isEqualTo(2);
        assertThat(content.path("createdAt").asText()).isNotBlank();

        assertThat(conversations.findById(conversationId)).get()
                .extracting(ConversationEntity::getType, ConversationEntity::getName,
                        ConversationEntity::getOwnerId, ConversationEntity::getAvatarFileId)
                .containsExactly(com.chat.modules.conversation.enums.ConversationTypeEnum.GROUP,
                        "Nhóm BTL", owner.getId(), avatarId);
        assertThat(conversationMembers.findByConversationIdAndUserId(conversationId, owner.getId()))
                .get().extracting(ConversationMemberEntity::getRole).isEqualTo(MemberRoleEnum.OWNER);
        assertThat(conversationMembers.findByConversationIdAndUserId(conversationId, member.getId()))
                .get().extracting(ConversationMemberEntity::getRole).isEqualTo(MemberRoleEnum.MEMBER);

        var memberDownload = restTemplate.exchange("/api/files/" + avatarId, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(memberSession.getRawToken())), byte[].class);
        assertThat(memberDownload.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberDownload.getBody()).isEqualTo(avatarContent);

        var outsiderDownload = restTemplate.exchange("/api/files/" + avatarId, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(outsiderSession.getRawToken())), byte[].class);
        assertThat(outsiderDownload.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        long countBeforeInvalidRequest = conversations.count();
        var selfOnly = restTemplate.exchange("/api/conversations/group", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Invalid group",
                        "memberIds", java.util.List.of(owner.getId())), groupHeaders),
                String.class);
        assertThat(selfOnly.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(conversations.count()).isEqualTo(countBeforeInvalidRequest);

        var unknownUser = restTemplate.exchange("/api/conversations/group", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Invalid group",
                        "memberIds", java.util.List.of(UUID.randomUUID())), groupHeaders),
                String.class);
        assertThat(unknownUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(conversations.count()).isEqualTo(countBeforeInvalidRequest);

        var invalidPayload = restTemplate.exchange("/api/conversations/group", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", " ", "memberIds", java.util.List.of()), groupHeaders),
                String.class);
        assertThat(invalidPayload.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidPayload.getBody()).contains("\"strCode\":\"VALIDATION_ERROR\"");
        assertThat(conversations.count()).isEqualTo(countBeforeInvalidRequest);
    }

    @Test
    void conversationListReturnsOnlyMembershipsOrderedByLatestActivity() throws Exception {
        var fixture = new TransactionTemplate(transactionManager).execute(status -> {
            var current = users.saveAndFlush(
                    new UserEntity("ConversationListCurrent", "hash-fixture", "List Current"));
            var other = users.saveAndFlush(
                    new UserEntity("ConversationListOther", "hash-fixture", "List Other"));
            var active = persist(ConversationEntity.group(current.getId(), "Active group", null));
            var currentMember = persist(new ConversationMemberEntity(
                    active.getId(), current.getId(), MemberRoleEnum.MEMBER, Instant.now()));
            persist(new ConversationMemberEntity(
                    active.getId(), other.getId(), MemberRoleEnum.OWNER, Instant.now()));
            var empty = persist(ConversationEntity.group(current.getId(), "Empty group", null));
            persist(new ConversationMemberEntity(
                    empty.getId(), current.getId(), MemberRoleEnum.OWNER, Instant.now()));
            var hidden = persist(ConversationEntity.group(other.getId(), "Hidden group", null));
            persist(new ConversationMemberEntity(
                    hidden.getId(), other.getId(), MemberRoleEnum.OWNER, Instant.now()));

            var readMessage = persist(new MessageEntity(
                    active.getId(), other.getId(), "list-read", "Đã đọc"));
            persist(new ConversationReadStateEntity(currentMember.getId(), readMessage.getId()));
            var file = new FileEntity(other.getId(), "latest.txt", "list/latest.txt", "text/plain", 6);
            file.bindToConversation(active.getId());
            persist(file);
            var latestMessage = persist(new MessageEntity(
                    active.getId(), other.getId(), "list-latest", "Mới nhất"));
            persist(new MessageAttachmentEntity(latestMessage.getId(), file.getId(), 0));
            return new ConversationListFixture(
                    current.getId(), active.getId(), empty.getId(), hidden.getId(),
                    other.getId(), latestMessage.getId(), file.getId());
        });

        var issuedSession = sessionService.create(fixture.currentUserId());
        var headers = sessionHeaders(issuedSession.getRawToken());
        var firstPage = restTemplate.exchange(
                "/api/conversations?page=0&size=1", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        var firstContent = objectMapper.readTree(firstPage.getBody()).path("content");
        assertThat(firstContent.path("totalElements").asLong()).isEqualTo(2);
        assertThat(firstContent.path("totalPages").asLong()).isEqualTo(2);
        assertThat(firstContent.path("data")).hasSize(1);
        var item = firstContent.path("data").get(0);
        assertThat(item.path("id").asText()).isEqualTo(fixture.activeConversationId().toString());
        assertThat(item.path("type").asText()).isEqualTo("GROUP");
        assertThat(item.path("memberCount").asLong()).isEqualTo(2);
        assertThat(item.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(item.path("lastMessage").path("id").asText())
                .isEqualTo(fixture.latestMessageId().toString());
        assertThat(item.path("lastMessage").path("sender").path("id").asText())
                .isEqualTo(fixture.senderId().toString());
        assertThat(item.path("lastMessage").path("sender").path("fullName").asText())
                .isEqualTo("List Other");
        assertThat(item.path("lastMessage").has("senderId")).isFalse();
        assertThat(item.path("lastMessage").path("attachments")).hasSize(1);
        assertThat(item.path("lastMessage").path("attachments").get(0).path("id").asText())
                .isEqualTo(fixture.fileId().toString());
        assertThat(firstPage.getBody()).doesNotContain(fixture.hiddenConversationId().toString());

        var secondPage = restTemplate.exchange(
                "/api/conversations?page=1&size=1", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        var secondItem = objectMapper.readTree(secondPage.getBody())
                .path("content").path("data").get(0);
        assertThat(secondItem.path("id").asText()).isEqualTo(fixture.emptyConversationId().toString());
        assertThat(secondItem.path("lastMessage").isNull()).isTrue();
        assertThat(secondItem.path("unreadCount").asLong()).isZero();

        var invalidPage = restTemplate.exchange(
                "/api/conversations?page=-1&size=101", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void directConversationListAndDetailIncludePeer() throws Exception {
        var fixture = new TransactionTemplate(transactionManager).execute(status -> {
            var current = users.saveAndFlush(
                    new UserEntity("DirectPeerCurrent", "hash-fixture", "Direct Current"));
            var peer = users.saveAndFlush(
                    new UserEntity("DirectPeerOther", "hash-fixture", "Direct Other"));
            var conversation = persist(ConversationEntity.direct(current.getId(), peer.getId()));
            var joinedAt = Instant.now();
            persist(new ConversationMemberEntity(
                    conversation.getId(), current.getId(), MemberRoleEnum.MEMBER, joinedAt));
            persist(new ConversationMemberEntity(
                    conversation.getId(), peer.getId(), MemberRoleEnum.MEMBER, joinedAt));
            return new DirectPeerFixture(current.getId(), peer.getId(), conversation.getId());
        });

        var session = sessionService.create(fixture.currentUserId());
        var headers = sessionHeaders(session.getRawToken());
        var list = restTemplate.exchange(
                "/api/conversations", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        var listItem = objectMapper.readTree(list.getBody())
                .path("content").path("data").get(0);
        assertThat(listItem.path("id").asText()).isEqualTo(fixture.conversationId().toString());
        assertThat(listItem.path("type").asText()).isEqualTo("DIRECT");
        assertThat(listItem.path("peer").path("id").asText()).isEqualTo(fixture.peerId().toString());
        assertThat(listItem.path("peer").path("username").asText()).isEqualTo("DirectPeerOther");
        assertThat(listItem.path("peer").path("fullName").asText()).isEqualTo("Direct Other");
        assertThat(listItem.path("peer").path("status").asText()).isEqualTo("OFFLINE");

        var detail = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId(), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        var detailContent = objectMapper.readTree(detail.getBody()).path("content");
        assertThat(detailContent.path("type").asText()).isEqualTo("DIRECT");
        assertThat(detailContent.path("peer").path("id").asText())
                .isEqualTo(fixture.peerId().toString());
        assertThat(detailContent.path("peer").path("username").asText())
                .isEqualTo("DirectPeerOther");
    }

    @Test
    void conversationDetailRequiresActiveMembership() throws Exception {
        var fixture = new TransactionTemplate(transactionManager).execute(status -> {
            var owner = users.saveAndFlush(
                    new UserEntity("ConversationDetailOwner", "hash-fixture", "Detail Owner"));
            var member = users.saveAndFlush(
                    new UserEntity("ConversationDetailMember", "hash-fixture", "Detail Member"));
            var formerMember = users.saveAndFlush(
                    new UserEntity("ConversationDetailFormer", "hash-fixture", "Detail Former"));
            var outsider = users.saveAndFlush(
                    new UserEntity("ConversationDetailOutsider", "hash-fixture", "Detail Outsider"));
            var conversation = persist(ConversationEntity.group(owner.getId(), "Nhóm chi tiết", null));
            persist(new ConversationMemberEntity(
                    conversation.getId(), owner.getId(), MemberRoleEnum.OWNER, Instant.now()));
            persist(new ConversationMemberEntity(
                    conversation.getId(), member.getId(), MemberRoleEnum.MEMBER, Instant.now()));
            var inactiveMembership = new ConversationMemberEntity(
                    conversation.getId(), formerMember.getId(), MemberRoleEnum.MEMBER,
                    Instant.now().minusSeconds(60));
            ReflectionTestUtils.setField(inactiveMembership, "leftAt", Instant.now());
            persist(inactiveMembership);
            return new ConversationDetailFixture(
                    owner.getId(), member.getId(), formerMember.getId(), outsider.getId(),
                    conversation.getId());
        });

        var memberSession = sessionService.create(fixture.memberId());
        var detail = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId(), HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(memberSession.getRawToken())), String.class);

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = objectMapper.readTree(detail.getBody()).path("content");
        assertThat(content.path("id").asText()).isEqualTo(fixture.conversationId().toString());
        assertThat(content.path("type").asText()).isEqualTo("GROUP");
        assertThat(content.path("name").asText()).isEqualTo("Nhóm chi tiết");
        assertThat(content.path("ownerId").asText()).isEqualTo(fixture.ownerId().toString());
        assertThat(content.path("memberCount").asLong()).isEqualTo(2);
        assertThat(content.path("createdAt").asText()).isNotBlank();

        var outsiderSession = sessionService.create(fixture.outsiderId());
        var forbidden = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId(), HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(outsiderSession.getRawToken())), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var formerSession = sessionService.create(fixture.formerMemberId());
        var formerMember = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId(), HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(formerSession.getRawToken())), String.class);
        assertThat(formerMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var missing = restTemplate.exchange(
                "/api/conversations/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(memberSession.getRawToken())), String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var invalidId = restTemplate.exchange(
                "/api/conversations/not-a-uuid", HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(memberSession.getRawToken())), String.class);
        assertThat(invalidId.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var unauthenticated = restTemplate.getForEntity(
                "/api/conversations/" + fixture.conversationId(), String.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateConversationRequiresGroupOwnerAndValidAvatar() throws Exception {
        var fixture = new TransactionTemplate(transactionManager).execute(status -> {
            var owner = users.saveAndFlush(
                    new UserEntity("ConversationUpdateOwner", "hash-fixture", "Update Owner"));
            var member = users.saveAndFlush(
                    new UserEntity("ConversationUpdateMember", "hash-fixture", "Update Member"));
            var group = persist(ConversationEntity.group(owner.getId(), "Tên nhóm cũ", null));
            persist(new ConversationMemberEntity(
                    group.getId(), owner.getId(), MemberRoleEnum.OWNER, Instant.now()));
            persist(new ConversationMemberEntity(
                    group.getId(), member.getId(), MemberRoleEnum.MEMBER, Instant.now()));
            var direct = persist(ConversationEntity.direct(owner.getId(), member.getId()));
            var validAvatar = persist(new FileEntity(
                    owner.getId(), "owner.png", "update/owner.png", "image/png", 10));
            var foreignAvatar = persist(new FileEntity(
                    member.getId(), "member.png", "update/member.png", "image/png", 10));
            return new UpdateConversationFixture(
                    owner.getId(), member.getId(), group.getId(), direct.getId(),
                    validAvatar.getId(), foreignAvatar.getId());
        });

        var ownerSession = sessionService.create(fixture.ownerId());
        var ownerHeaders = sessionHeaders(ownerSession.getRawToken());
        ownerHeaders.setContentType(MediaType.APPLICATION_JSON);
        var updated = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "  Nhóm đã đổi  ",
                        "avatarFileId", fixture.validAvatarId()), ownerHeaders),
                String.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = objectMapper.readTree(updated.getBody()).path("content");
        assertThat(content.path("id").asText()).isEqualTo(fixture.groupId().toString());
        assertThat(content.path("type").asText()).isEqualTo("GROUP");
        assertThat(content.path("name").asText()).isEqualTo("Nhóm đã đổi");
        assertThat(content.path("avatarFileId").asText()).isEqualTo(fixture.validAvatarId().toString());
        assertThat(content.path("ownerId").asText()).isEqualTo(fixture.ownerId().toString());
        assertThat(content.path("memberCount").asLong()).isEqualTo(2);
        assertThat(conversations.findById(fixture.groupId())).get()
                .extracting(ConversationEntity::getName, ConversationEntity::getAvatarFileId)
                .containsExactly("Nhóm đã đổi", fixture.validAvatarId());

        var clearAvatar = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>("{\"name\":\"Nhóm không avatar\",\"avatarFileId\":null}", ownerHeaders),
                String.class);
        assertThat(clearAvatar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(clearAvatar.getBody())
                .path("content").path("avatarFileId").isNull()).isTrue();

        var memberSession = sessionService.create(fixture.memberId());
        var memberHeaders = sessionHeaders(memberSession.getRawToken());
        memberHeaders.setContentType(MediaType.APPLICATION_JSON);
        var forbiddenMember = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>("{\"name\":\"Không được sửa\",\"avatarFileId\":null}", memberHeaders),
                String.class);
        assertThat(forbiddenMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var direct = restTemplate.exchange(
                "/api/conversations/" + fixture.directId(), HttpMethod.PUT,
                new HttpEntity<>("{\"name\":\"Direct\",\"avatarFileId\":null}", ownerHeaders),
                String.class);
        assertThat(direct.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var missingConversation = restTemplate.exchange(
                "/api/conversations/" + UUID.randomUUID(), HttpMethod.PUT,
                new HttpEntity<>("{\"name\":\"Không tồn tại\",\"avatarFileId\":null}", ownerHeaders),
                String.class);
        assertThat(missingConversation.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var missingFile = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "File không tồn tại", "avatarFileId", UUID.randomUUID()), ownerHeaders),
                String.class);
        assertThat(missingFile.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var forbiddenFile = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "File sai quyền", "avatarFileId", fixture.foreignAvatarId()), ownerHeaders),
                String.class);
        assertThat(forbiddenFile.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var missingAvatarKey = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Thiếu key"), ownerHeaders), String.class);
        assertThat(missingAvatarKey.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var invalidName = restTemplate.exchange(
                "/api/conversations/" + fixture.groupId(), HttpMethod.PUT,
                new HttpEntity<>("{\"name\":\" \",\"avatarFileId\":null}", ownerHeaders),
                String.class);
        assertThat(invalidName.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void conversationMembersReturnsOnlyActiveMembersWithPagination() throws Exception {
        var fixture = new TransactionTemplate(transactionManager).execute(status -> {
            var owner = users.saveAndFlush(
                    new UserEntity("MembersApiOwner", "hash-fixture", "Members Owner"));
            var current = users.saveAndFlush(
                    new UserEntity("MembersApiCurrent", "hash-fixture", "Members Current"));
            var other = users.saveAndFlush(
                    new UserEntity("MembersApiOther", "hash-fixture", "Members Other"));
            var former = users.saveAndFlush(
                    new UserEntity("MembersApiFormer", "hash-fixture", "Members Former"));
            var outsider = users.saveAndFlush(
                    new UserEntity("MembersApiOutsider", "hash-fixture", "Members Outsider"));
            var conversation = persist(ConversationEntity.group(owner.getId(), "Nhóm thành viên", null));
            var now = Instant.now();
            persist(new ConversationMemberEntity(
                    conversation.getId(), owner.getId(), MemberRoleEnum.OWNER, now.minusSeconds(300)));
            persist(new ConversationMemberEntity(
                    conversation.getId(), current.getId(), MemberRoleEnum.MEMBER, now.minusSeconds(240)));
            persist(new ConversationMemberEntity(
                    conversation.getId(), other.getId(), MemberRoleEnum.MEMBER, now.minusSeconds(180)));
            var formerMembership = new ConversationMemberEntity(
                    conversation.getId(), former.getId(), MemberRoleEnum.MEMBER, now.minusSeconds(360));
            ReflectionTestUtils.setField(formerMembership, "leftAt", now.minusSeconds(60));
            persist(formerMembership);
            return new ConversationMembersFixture(
                    owner.getId(), current.getId(), other.getId(), former.getId(),
                    outsider.getId(), conversation.getId());
        });

        var currentSession = sessionService.create(fixture.currentUserId());
        var headers = sessionHeaders(currentSession.getRawToken());
        var firstPage = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId() + "/members?page=0&size=2",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        var firstContent = objectMapper.readTree(firstPage.getBody()).path("content");
        assertThat(firstContent.path("totalElements").asLong()).isEqualTo(3);
        assertThat(firstContent.path("totalPages").asLong()).isEqualTo(2);
        assertThat(firstContent.path("data")).hasSize(2);
        var ownerItem = firstContent.path("data").get(0);
        assertThat(ownerItem.path("user").path("id").asText())
                .isEqualTo(fixture.ownerId().toString());
        assertThat(ownerItem.path("user").path("username").asText()).isEqualTo("MembersApiOwner");
        assertThat(ownerItem.path("user").path("status").asText()).isEqualTo("OFFLINE");
        assertThat(ownerItem.path("role").asText()).isEqualTo("OWNER");
        assertThat(ownerItem.path("joinedAt").asText()).isNotBlank();
        assertThat(firstContent.toString()).doesNotContain(fixture.formerUserId().toString());

        var secondPage = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId() + "/members?page=1&size=2",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        var secondData = objectMapper.readTree(secondPage.getBody()).path("content").path("data");
        assertThat(secondData).hasSize(1);
        assertThat(secondData.get(0).path("user").path("id").asText())
                .isEqualTo(fixture.otherUserId().toString());
        assertThat(secondData.get(0).path("role").asText()).isEqualTo("MEMBER");

        var outsiderSession = sessionService.create(fixture.outsiderId());
        var forbidden = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId() + "/members",
                HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(outsiderSession.getRawToken())), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var missing = restTemplate.exchange(
                "/api/conversations/" + UUID.randomUUID() + "/members",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var invalidPagination = restTemplate.exchange(
                "/api/conversations/" + fixture.conversationId() + "/members?page=-1&size=101",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(invalidPagination.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

    private record ConversationListFixture(
            UUID currentUserId,
            UUID activeConversationId,
            UUID emptyConversationId,
            UUID hiddenConversationId,
            UUID senderId,
            UUID latestMessageId,
            UUID fileId) {
    }

    private record DirectPeerFixture(
            UUID currentUserId,
            UUID peerId,
            UUID conversationId) {
    }

    private record ConversationDetailFixture(
            UUID ownerId,
            UUID memberId,
            UUID formerMemberId,
            UUID outsiderId,
            UUID conversationId) {
    }

    private record UpdateConversationFixture(
            UUID ownerId,
            UUID memberId,
            UUID groupId,
            UUID directId,
            UUID validAvatarId,
            UUID foreignAvatarId) {
    }

    private record ConversationMembersFixture(
            UUID ownerId,
            UUID currentUserId,
            UUID otherUserId,
            UUID formerUserId,
            UUID outsiderId,
            UUID conversationId) {
    }
}
