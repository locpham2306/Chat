CREATE TABLE files (
    id BINARY(16) NOT NULL PRIMARY KEY,
    uploader_id BINARY(16) NOT NULL,
    conversation_id BINARY(16) NULL,
    name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_files_conversation_id UNIQUE (conversation_id, id),
    CONSTRAINT fk_files_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT ck_files_size CHECK (size > 0),
    INDEX idx_files_uploader_created (uploader_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE conversations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    type VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(100) NULL,
    owner_id BINARY(16) NULL,
    avatar_file_id BINARY(16) NULL,
    direct_user_low_id BINARY(16) NULL,
    direct_user_high_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_conversations_direct_pair UNIQUE (direct_user_low_id, direct_user_high_id),
    CONSTRAINT fk_conversations_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_conversations_avatar FOREIGN KEY (avatar_file_id) REFERENCES files(id),
    CONSTRAINT fk_conversations_direct_low FOREIGN KEY (direct_user_low_id) REFERENCES users(id),
    CONSTRAINT fk_conversations_direct_high FOREIGN KEY (direct_user_high_id) REFERENCES users(id),
    CONSTRAINT ck_conversations_shape CHECK (
        (type = 'DIRECT' AND direct_user_low_id IS NOT NULL AND direct_user_high_id IS NOT NULL
         AND direct_user_low_id < direct_user_high_id AND owner_id IS NULL AND name IS NULL AND avatar_file_id IS NULL)
        OR
        (type = 'GROUP' AND owner_id IS NOT NULL AND name IS NOT NULL AND CHAR_LENGTH(TRIM(name)) > 0
         AND direct_user_low_id IS NULL AND direct_user_high_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

ALTER TABLE files ADD CONSTRAINT fk_files_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations(id);
ALTER TABLE users ADD CONSTRAINT fk_users_avatar
    FOREIGN KEY (avatar_file_id) REFERENCES files(id);

CREATE TABLE messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    client_message_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    content TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_messages_sender_client UNIQUE (sender_id, client_message_id),
    CONSTRAINT uk_messages_conversation_id UNIQUE (conversation_id, id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT ck_messages_client_id CHECK (CHAR_LENGTH(TRIM(client_message_id)) > 0),
    INDEX idx_messages_history (conversation_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE conversation_members (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    left_at DATETIME(6) NULL,
    last_read_message_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_members_conversation_user UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_members_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_members_last_read FOREIGN KEY (conversation_id, last_read_message_id)
        REFERENCES messages(conversation_id, id),
    CONSTRAINT ck_members_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT ck_members_left_at CHECK (left_at IS NULL OR left_at >= joined_at),
    INDEX idx_members_user_active (user_id, left_at, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE message_attachments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    message_id BINARY(16) NOT NULL,
    file_id BINARY(16) NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_attachments_message_file UNIQUE (message_id, file_id),
    CONSTRAINT uk_attachments_message_position UNIQUE (message_id, position),
    CONSTRAINT fk_attachments_message FOREIGN KEY (conversation_id, message_id)
        REFERENCES messages(conversation_id, id),
    CONSTRAINT fk_attachments_file FOREIGN KEY (conversation_id, file_id)
        REFERENCES files(conversation_id, id),
    CONSTRAINT ck_attachments_position CHECK (position >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;
