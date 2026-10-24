package com.chat.modules.auth.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.BusinessException;
import com.chat.modules.auth.config.SessionCookieProperties;
import com.chat.modules.auth.security.SessionTokenGenerator;
import com.chat.modules.auth.service.IssuedSession;
import com.chat.modules.auth.service.SessionCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SessionCookieServiceImpl implements SessionCookieService {
    private static final String COOKIE_NAME = "token";
    private final SessionCookieProperties properties;
    private final SessionTokenGenerator tokens;
    private final Clock clock;

    @Override
    public void write(HttpServletResponse response, IssuedSession session) {
        if (!tokens.isValidFormat(session.getRawToken())) {
            throw new IllegalArgumentException("Invalid session token format");
        }
        var remaining = Duration.between(clock.instant(), session.getExpiresAt());
        if (remaining.isNegative() || remaining.isZero()) {
            throw new IllegalArgumentException("Cannot set an expired session cookie");
        }
        long seconds = remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(session.getRawToken()).maxAge(seconds).build().toString());
    }

    @Override
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("").maxAge(Duration.ZERO).build().toString());
    }

    @Override
    public String read(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (var candidate : request.getCookies()) {
                if (COOKIE_NAME.equals(candidate.getName())) {
                    // Ambiguous same-name cookies must not select an arbitrary session.
                    if (token != null) {
                        throw invalidRequest();
                    }
                    token = candidate.getValue();
                }
            }
        }
        return token;
    }

    @Override
    public String resolveLogoutToken(HttpServletRequest request, String bodyToken) {
        var cookieToken = read(request);
        if (cookieToken != null && bodyToken != null && !cookieToken.equals(bodyToken)) {
            throw invalidRequest();
        }
        var token = cookieToken != null ? cookieToken : bodyToken;
        if (!tokens.isValidFormat(token)) {
            throw invalidRequest();
        }
        return token;
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true).secure(properties.secure()).sameSite(properties.sameSite()).path("/");
    }

    private BusinessException invalidRequest() {
        return new BadRequestException(ErrorConstants.VALIDATION_ERROR);
    }
}
