package com.chat.modules.file.service;

import org.springframework.core.io.Resource;

public record FileDownload(
        String name,
        String contentType,
        long size,
        Resource resource) {
}
