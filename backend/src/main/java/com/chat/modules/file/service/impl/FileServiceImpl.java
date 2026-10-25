package com.chat.modules.file.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.config.FileProperties;
import com.chat.exception.BadRequestException;
import com.chat.exception.NoPermissionException;
import com.chat.exception.NotFoundException;
import com.chat.exception.PayloadTooLargeException;
import com.chat.exception.UnsupportedFileTypeException;
import com.chat.integration.storage.FileStorage;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.file.dto.response.FileResponse;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.file.service.FileDownload;
import com.chat.modules.file.service.FileService;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final Logger LOG = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository files;
    private final UserRepository users;
    private final ConversationMemberRepository members;
    private final FileStorage storage;
    private final FileProperties properties;

    @Override
    @Transactional
    public FileResponse upload(UUID uploaderId, MultipartFile file) {
        validate(file);
        String name = fileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        String storageKey = UUID.randomUUID().toString();

        try (var input = file.getInputStream()) {
            storage.store(storageKey, input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read uploaded file", exception);
        }
        deleteStoredFileAfterRollback(storageKey);

        var saved = files.saveAndFlush(
                new FileEntity(uploaderId, name, storageKey, contentType, file.getSize()));
        return new FileResponse(saved.getId(), saved.getName(), saved.getContentType(),
                saved.getSize(), "/api/files/" + saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload download(UUID userId, UUID fileId) {
        var file = files.findById(fileId)
                .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
        if (!canRead(userId, file)) {
            throw new NoPermissionException(ErrorConstants.FORBIDDEN);
        }
        return new FileDownload(file.getName(), file.getContentType(), file.getSize(),
                storage.load(file.getStorageKey()));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() < 1) {
            throw new BadRequestException("file.empty");
        }
        if (file.getSize() > properties.maxSize().toBytes()) {
            throw new PayloadTooLargeException("file.tooLarge");
        }
        fileName(file.getOriginalFilename());
        normalizeContentType(file.getContentType());
    }

    private String fileName(String originalName) {
        if (originalName == null) {
            throw new BadRequestException("file.invalidName");
        }
        String normalized = originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (normalized.isEmpty() || normalized.length() > MAX_FILE_NAME_LENGTH) {
            throw new BadRequestException("file.invalidName");
        }
        return normalized;
    }

    private String normalizeContentType(String rawContentType) {
        try {
            var mediaType = MediaType.parseMediaType(rawContentType == null ? "" : rawContentType);
            String contentType = (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase(Locale.ROOT);
            if (!properties.allows(contentType)) {
                throw new UnsupportedFileTypeException("file.unsupportedType");
            }
            return contentType;
        } catch (InvalidMediaTypeException exception) {
            throw new UnsupportedFileTypeException("file.unsupportedType");
        }
    }

    private boolean canRead(UUID userId, FileEntity file) {
        if (userId.equals(file.getUploaderId()) || users.existsByAvatarFileId(file.getId())) {
            return true;
        }
        return file.getConversationId() != null
                && members.existsByConversationIdAndUserIdAndLeftAtIsNull(file.getConversationId(), userId);
    }

    private void deleteStoredFileAfterRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        storage.delete(storageKey);
                    } catch (RuntimeException exception) {
                        LOG.warn("Cannot remove stored file after transaction rollback");
                    }
                }
            }
        });
    }
}
