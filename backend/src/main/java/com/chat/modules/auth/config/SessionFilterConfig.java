package com.chat.modules.auth.config;

import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionCookieService;
import com.chat.modules.auth.service.SessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SessionFilterConfig {

    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionAuthenticationFilter(
            SessionCookieService cookies,
            SessionService sessions,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        var registration = new FilterRegistrationBean<>(
                new SessionAuthenticationFilter(cookies, sessions, exceptionResolver));
        registration.addUrlPatterns("/api/*", "/ws");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
