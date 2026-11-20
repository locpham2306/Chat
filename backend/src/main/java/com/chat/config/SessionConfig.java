package com.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Đăng ký cấu hình cookie session để Spring có thể inject vào service.
@Configuration
@EnableConfigurationProperties(SessionCookieProperties.class)
public class SessionConfig {
}
