package com.chat.modules.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SessionCookieProperties.class)
public class SessionConfig {
}
