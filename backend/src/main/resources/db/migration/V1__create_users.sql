CREATE TABLE users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    username VARCHAR(50) COLLATE utf8mb4_0900_as_ci NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_file_id BINARY(16) NULL,
    last_seen DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;
