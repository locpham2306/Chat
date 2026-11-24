package com.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Cấu hình cách băm và kiểm tra mật khẩu.
@Configuration
public class PasswordConfig {

    // Dùng BCrypt để băm mật khẩu khi đăng ký và kiểm tra khi đăng nhập.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
