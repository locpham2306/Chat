package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Đọc Secure và SameSite của cookie session từ app.session.cookie.
@ConfigurationProperties("app.session.cookie")
public record SessionCookieProperties(boolean secure, String sameSite) {

    // Kiểm tra SameSite hợp lệ; SameSite=None thì bắt buộc cookie phải Secure.
    public SessionCookieProperties {
        if (!"Lax".equals(sameSite) && !"Strict".equals(sameSite) && !"None".equals(sameSite)) {
            throw new IllegalArgumentException("Cookie SameSite must be Lax, Strict or None");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requires a Secure cookie");
        }
    }
}
