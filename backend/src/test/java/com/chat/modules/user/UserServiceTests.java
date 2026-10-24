package com.chat.modules.user;

import com.chat.exception.BusinessException;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.user.dto.request.UpdateProfileRequest;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final FileRepository files = mock(FileRepository.class);
    private final UserServiceImpl service = new UserServiceImpl(users, files, new UserMapper());

    @Test
    void updateProfileTrimsFullNameAndAllowsClearingAvatar() {
        var userId = UUID.randomUUID();
        var user = user(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var response = service.updateCurrentUser(userId, new UpdateProfileRequest("  Nguyễn Văn Bình  ", null));

        assertThat(response.fullName()).isEqualTo("Nguyễn Văn Bình");
        assertThat(response.avatarFileId()).isNull();
        verifyNoInteractions(files);
    }

    @Test
    void updateProfileAcceptsOwnedUnboundImage() {
        var userId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        var user = user(userId);
        var avatar = file(avatarId, userId, "image/png");
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(files.findById(avatarId)).thenReturn(Optional.of(avatar));

        var response = service.updateCurrentUser(userId, new UpdateProfileRequest("An", avatarId));

        assertThat(response.avatarFileId()).isEqualTo(avatarId);
    }

    @Test
    void updateProfileRejectsMissingOrUnauthorizedAvatar() {
        var userId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));

        assertStatus(() -> service.updateCurrentUser(userId, new UpdateProfileRequest("An", avatarId)),
                HttpStatus.NOT_FOUND);

        when(files.findById(avatarId)).thenReturn(Optional.of(file(avatarId, UUID.randomUUID(), "image/png")));
        assertStatus(() -> service.updateCurrentUser(userId, new UpdateProfileRequest("An", avatarId)),
                HttpStatus.FORBIDDEN);

        when(files.findById(avatarId)).thenReturn(Optional.of(file(avatarId, userId, "text/plain")));
        assertStatus(() -> service.updateCurrentUser(userId, new UpdateProfileRequest("An", avatarId)),
                HttpStatus.FORBIDDEN);

        var boundImage = file(avatarId, userId, "image/png");
        boundImage.bindToConversation(UUID.randomUUID());
        when(files.findById(avatarId)).thenReturn(Optional.of(boundImage));
        assertStatus(() -> service.updateCurrentUser(userId, new UpdateProfileRequest("An", avatarId)),
                HttpStatus.FORBIDDEN);
    }

    private UserEntity user(UUID id) {
        var user = new UserEntity("an", "hash", "An");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private FileEntity file(UUID id, UUID uploaderId, String contentType) {
        var file = new FileEntity(uploaderId, "avatar", "avatars/" + id, contentType, 1);
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
