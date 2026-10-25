package com.chat.integration.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface FileStorage {
    void store(String storageKey, InputStream content);

    Resource load(String storageKey);

    void delete(String storageKey);
}
