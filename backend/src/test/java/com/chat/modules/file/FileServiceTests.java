package com.chat.modules.file;

import com.chat.config.FileProperties;
import com.chat.exception.BusinessException;
import com.chat.integration.storage.FileStorage;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.file.service.impl.FileServiceImpl;
import com.chat.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileServiceTests {
    private final FileRepository files = mock(FileRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ConversationMemberRepository members = mock(ConversationMemberRepository.class);
    private final FileStorage storage = mock(FileStorage.class);
    private final FileProperties properties = new FileProperties(
            Path.of("uploads"), DataSize.ofMegabytes(20));
    private final FileServiceImpl service = new FileServiceImpl(files, users, members, storage, properties);

    @Test
    void uploadStoresPayloadAndPersistsOnlySafeMetadata() {
        var uploaderId = UUID.randomUUID();
        var fileId = UUID.randomUUID();
        var upload = new MockMultipartFile(
                "file", "folder\\xin-chào.txt", "text/plain;charset=UTF-8", "Xin chào".getBytes());
        when(files.saveAndFlush(any(FileEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, FileEntity.class);
            ReflectionTestUtils.setField(entity, "id", fileId);
            return entity;
        });

        var response = service.upload(uploaderId, upload);

        assertThat(response.id()).isEqualTo(fileId);
        assertThat(response.name()).isEqualTo("xin-chào.txt");
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.size()).isEqualTo(upload.getSize());
        assertThat(response.downloadUrl()).isEqualTo("/api/files/" + fileId);
        verify(storage).store(anyString(), any());
        verify(files).saveAndFlush(any(FileEntity.class));
    }

    @Test
    void uploadRejectsEmptyAndOversizedFiles() {
        assertStatus(() -> service.upload(UUID.randomUUID(),
                new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])), HttpStatus.BAD_REQUEST);

        var oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(DataSize.ofMegabytes(20).toBytes() + 1);
        assertStatus(() -> service.upload(UUID.randomUUID(), oversized), HttpStatus.PAYLOAD_TOO_LARGE);

        verifyNoInteractions(storage);
        verify(files, never()).saveAndFlush(any());
    }

    @Test
    void uploadAcceptsAnyFileTypeAndFallsBackForInvalidContentType() {
        when(files.saveAndFlush(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var executable = service.upload(UUID.randomUUID(),
                new MockMultipartFile(
                        "file", "program.exe", "application/x-msdownload", new byte[]{1}));
        var unknown = service.upload(UUID.randomUUID(),
                new MockMultipartFile(
                        "file", "unknown.bin", "not-a-media-type", new byte[]{2}));

        assertThat(executable.contentType()).isEqualTo("application/x-msdownload");
        assertThat(unknown.contentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    @Test
    void downloadAllowsUploaderUserAvatarGroupAvatarAndActiveConversationMember() {
        var uploaderId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var file = file(uploaderId);
        var resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(files.findById(file.getId())).thenReturn(Optional.of(file));
        when(storage.load(file.getStorageKey())).thenReturn(resource);

        assertThat(service.download(uploaderId, file.getId()).resource()).isSameAs(resource);

        when(users.existsByAvatarFileId(file.getId())).thenReturn(true);
        assertThat(service.download(viewerId, file.getId()).resource()).isSameAs(resource);

        when(users.existsByAvatarFileId(file.getId())).thenReturn(false);
        when(members.countActiveMembershipsForConversationAvatar(viewerId, file.getId())).thenReturn(1L);
        assertThat(service.download(viewerId, file.getId()).resource()).isSameAs(resource);

        when(members.countActiveMembershipsForConversationAvatar(viewerId, file.getId())).thenReturn(0L);
        var conversationId = UUID.randomUUID();
        file.bindToConversation(conversationId);
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, viewerId)).thenReturn(true);
        assertThat(service.download(viewerId, file.getId()).resource()).isSameAs(resource);
    }

    @Test
    void downloadRejectsUnknownAndUnauthorizedFiles() {
        var userId = UUID.randomUUID();
        var missingId = UUID.randomUUID();
        assertStatus(() -> service.download(userId, missingId), HttpStatus.NOT_FOUND);

        var file = file(UUID.randomUUID());
        when(files.findById(file.getId())).thenReturn(Optional.of(file));
        assertStatus(() -> service.download(userId, file.getId()), HttpStatus.FORBIDDEN);
        verify(storage, never()).load(anyString());
    }

    private FileEntity file(UUID uploaderId) {
        var id = UUID.randomUUID();
        var file = new FileEntity(uploaderId, "file.txt", UUID.randomUUID().toString(), "text/plain", 3);
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
