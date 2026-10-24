package com.chat.modules.auth.security;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.exception.BusinessException;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_ATTRIBUTE = "com.chat.auth.session";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register", "/api/auth/login", "/api/auth/logout");

    private final SessionCookieService cookies;
    private final SessionService sessions;
    private final HandlerExceptionResolver exceptionResolver;

    public SessionAuthenticationFilter(
            SessionCookieService cookies,
            SessionService sessions,
            HandlerExceptionResolver exceptionResolver) {
        this.cookies = cookies;
        this.sessions = sessions;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || PUBLIC_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            var identity = sessions.findActive(cookies.read(request)).orElseThrow(this::unauthenticated);
            request.setAttribute(SESSION_ATTRIBUTE, identity);
        } catch (BusinessException exception) {
            exceptionResolver.resolveException(request, response, null,
                    exception.getStatus() == HttpStatus.UNAUTHORIZED ? exception : unauthenticated());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private BusinessException unauthenticated() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, ErrorConstants.UNAUTHENTICATED);
    }
}
