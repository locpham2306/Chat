package com.chat.modules.user.entity;

import com.chat.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_seen")
    private Instant lastSeen;

    protected UserEntity() {
    }

    /** passwordHash must already be encoded by the authentication service. */
    public UserEntity(String username, String passwordHash, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public void updateProfile(String fullName, UUID avatarFileId) {
        if (fullName == null || fullName.isBlank() || fullName.length() > 100) {
            throw new IllegalArgumentException("Invalid full name");
        }
        this.fullName = fullName;
        this.avatarFileId = avatarFileId;
    }

    public void markLastSeen(Instant lastSeen) {
        this.lastSeen = java.util.Objects.requireNonNull(lastSeen);
    }
}
