-- Test-only schema. This file is not packaged in the application jar.
CREATE TABLE persistence_probe (
    id BIGINT NOT NULL PRIMARY KEY,
    content VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
