package com.chat.modules.auth.security;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.auth.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_ATTRIBUTE = "com.chat.auth.session";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register", "/api/auth/login", "/api/auth/logout");

    private final SessionCookieService sessionCookieService;
    private final SessionService sessionService;
    // Filter chạy trước Controller nên cần chủ động chuyển lỗi cho GlobalExceptionHandler.
    private final HandlerExceptionResolver exceptionResolver;

    public SessionAuthenticationFilter(
            SessionCookieService sessionCookieService,
            SessionService sessionService,
            HandlerExceptionResolver exceptionResolver) {
        this.sessionCookieService = sessionCookieService;
        this.sessionService = sessionService;
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
            // Kiểm tra token trong cookie và lấy thông tin phiên đăng nhập hiện tại.
            String token = sessionCookieService.read(request);
            SessionIdentity identity = sessionService.findActive(token)
                    .orElseThrow(this::unauthenticated);
            // Controller lấy lại identity qua @RequestAttribute để biết user đang đăng nhập.
            request.setAttribute(SESSION_ATTRIBUTE, identity);
        } catch (BusinessException exception) {
            BusinessException authenticationException = exception;
            if (exception.getStatus().value() != HttpServletResponse.SC_UNAUTHORIZED) {
                authenticationException = unauthenticated();
            }
            exceptionResolver.resolveException(
                    request, response, null, authenticationException);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private BusinessException unauthenticated() {
        return BusinessException.unauthenticated(ErrorConstants.UNAUTHENTICATED);
    }
}
