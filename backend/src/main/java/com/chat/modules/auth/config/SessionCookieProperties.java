package com.chat.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.session.cookie")
public record SessionCookieProperties(boolean secure, String sameSite) {
    public SessionCookieProperties {
        if (!"Lax".equals(sameSite) && !"Strict".equals(sameSite) && !"None".equals(sameSite)) {
            throw new IllegalArgumentException("Cookie SameSite must be Lax, Strict or None");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requires a Secure cookie");
        }
    }
}
