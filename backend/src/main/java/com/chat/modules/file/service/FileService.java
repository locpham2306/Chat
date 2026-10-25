package com.chat.modules.file.service;

import com.chat.modules.file.dto.response.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileService {
    FileResponse upload(UUID uploaderId, MultipartFile file);

    FileDownload download(UUID userId, UUID fileId);
}
