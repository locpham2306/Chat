package com.chat.modules.auth;

import com.chat.exception.BusinessException;
import com.chat.modules.auth.config.SessionCookieProperties;
import com.chat.modules.auth.entity.SessionEntity;
import com.chat.modules.auth.repository.SessionRepository;
import com.chat.modules.auth.security.SessionTokenGenerator;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.impl.SessionCookieServiceImpl;
import com.chat.modules.auth.service.impl.SessionServiceImpl;
import com.chat.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionFoundationTests {
    private final Instant now = Instant.parse("2026-10-20T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final SessionTokenGenerator tokens = new SessionTokenGenerator();
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SessionServiceImpl service = new SessionServiceImpl(sessions, users, tokens, clock);
    private final SessionCookieServiceImpl cookies = new SessionCookieServiceImpl(
            new SessionCookieProperties(true, "Lax"), tokens, clock);

    @Test
    void tokensHave256BitsAndCanonicalEncodingAndSha256Digest() {
        var token = tokens.generate();
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(tokens.isValidFormat(token)).isTrue();
        assertThat(tokens.generate()).isNotEqualTo(token);
        assertThat(tokens.hash("A".repeat(43)))
                .isEqualTo("0f007385b6f9d4b7eeb2748605afe1a984a0a3bfa3f014d09e2a784ce9e5cd1a");
    }

    @Test
    void malformedTokensNeverReachDatabase() {
        for (String token : new String[]{"", " ", "a".repeat(42), "A".repeat(42) + "B", "A".repeat(43) + "="}) {
            assertThat(tokens.isValidFormat(token)).isFalse();
            assertThat(service.findActive(token)).isEmpty();
            service.revoke(token);
        }
        assertThat(service.findActive((String) null)).isEmpty();
        service.revoke((String) null);
        verifyNoInteractions(sessions);
    }

    @Test
    void creationStoresOnlyHashWithFixed365DayExpiry() {
        var userId = UUID.randomUUID();
        when(users.existsById(userId)).thenReturn(true);
        when(sessions.saveAndFlush(any(SessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var issued = service.create(userId);
        var capture = ArgumentCaptor.forClass(SessionEntity.class);
        verify(sessions).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getTokenHash()).isEqualTo(tokens.hash(issued.getRawToken()));
        assertThat(capture.getValue().getUserId()).isEqualTo(userId);
        assertThat(capture.getValue().getCreatedAt()).isEqualTo(now);
        assertThat(issued.getExpiresAt()).isEqualTo(now.plus(Duration.ofDays(365)));
        assertThatThrownBy(() -> service.create(UUID.randomUUID())).isInstanceOf(BusinessException.class);
    }

    @Test
    void lookupUsesCurrentTimeAndDoesNotRenewSession() {
        var token = tokens.generate();
        var userId = UUID.randomUUID();
        var entity = new SessionEntity(userId, tokens.hash(token), now.minusSeconds(60), now.plusSeconds(60));
        when(sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokens.hash(token), now))
                .thenReturn(Optional.of(entity));
        var identity = service.findActive(token).orElseThrow();
        assertThat(identity.userId()).isEqualTo(userId);
        assertThat(identity.expiresAt()).isEqualTo(now.plusSeconds(60));
        verify(sessions).findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokens.hash(token), now);
        verifyNoMoreInteractions(sessions);
        service.revoke(token);
        verify(sessions).revokeByTokenHash(tokens.hash(token), now);
    }

    @Test
    void rawTokenIsExcludedFromSerializationAndToString() throws Exception {
        var issued = new IssuedSession(UUID.randomUUID(), now.plusSeconds(60), tokens.generate());
        var json = new ObjectMapper().findAndRegisterModules().writeValueAsString(issued);
        assertThat(json).doesNotContain(issued.getRawToken(), "rawToken");
        assertThat(issued.toString()).doesNotContain(issued.getRawToken());
    }

    @Test
    void cookieUsesRemainingLifetimeAndClearUsesSameScope() {
        var token = tokens.generate();
        var response = new MockHttpServletResponse();
        response.addHeader(HttpHeaders.SET_COOKIE, "another=value");
        cookies.write(response, new IssuedSession(UUID.randomUUID(), now.plus(Duration.ofDays(365)), token));
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2);
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE).get(1))
                .contains("token=" + token, "Path=/", "Max-Age=31536000", "HttpOnly", "Secure", "SameSite=Lax");
        var cleared = new MockHttpServletResponse();
        cookies.clear(cleared);
        assertThat(cleared.getHeader(HttpHeaders.SET_COOKIE))
                .contains("token=;", "Path=/", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Lax");
        assertThatThrownBy(() -> cookies.write(response, new IssuedSession(UUID.randomUUID(), now, token)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logoutAcceptsCookieOrBodyButRejectsMissingConflictingAndDuplicateCookies() {
        var token = tokens.generate();
        var request = new MockHttpServletRequest();
        assertThat(cookies.resolveLogoutToken(request, token)).isEqualTo(token);
        assertBadRequest(() -> cookies.resolveLogoutToken(request, null));
        request.setCookies(new Cookie("token", token));
        assertThat(cookies.resolveLogoutToken(request, null)).isEqualTo(token);
        assertThat(cookies.resolveLogoutToken(request, token)).isEqualTo(token);
        assertBadRequest(() -> cookies.resolveLogoutToken(request, tokens.generate()));
        request.setCookies(new Cookie("token", token), new Cookie("token", token));
        assertBadRequest(() -> cookies.resolveLogoutToken(request, null));
    }

    @Test
    void crossSiteCookieRequiresSecureAndLocalHttpCanBeConfiguredExplicitly() {
        assertThatThrownBy(() -> new SessionCookieProperties(false, "None"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionCookieProperties(true, "invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        var localCookies = new SessionCookieServiceImpl(new SessionCookieProperties(false, "Lax"), tokens, clock);
        var response = new MockHttpServletResponse();
        localCookies.write(response, new IssuedSession(UUID.randomUUID(), now.plusSeconds(60), tokens.generate()));
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("HttpOnly").doesNotContain("Secure");
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
