package com.chat.config;

import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Đăng ký filter xác thực session cho HTTP API và WebSocket handshake.
@Configuration
public class SessionFilterConfig {

    // Đọc cookie, kiểm tra session rồi gắn danh tính người dùng vào request.
    // Filter chạy sau CORS để request OPTIONS không bị bắt đăng nhập.
    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionAuthenticationFilter(
            SessionCookieService sessionCookieService,
            SessionService sessionService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        FilterRegistrationBean<SessionAuthenticationFilter> registration = new FilterRegistrationBean<>(
                new SessionAuthenticationFilter(sessionCookieService, sessionService, exceptionResolver));
        registration.addUrlPatterns("/api/*", "/ws");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
