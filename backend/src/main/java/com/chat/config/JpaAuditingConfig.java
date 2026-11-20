package com.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Bật tự động ghi createdAt và updatedAt cho các entity kế thừa BaseEntity.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
