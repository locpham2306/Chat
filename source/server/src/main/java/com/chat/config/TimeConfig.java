package com.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Cung cấp thời gian UTC dùng chung cho toàn backend.
@Configuration
public class TimeConfig {

    // Các service inject Clock này thay vì tự gọi thời gian hệ thống.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
