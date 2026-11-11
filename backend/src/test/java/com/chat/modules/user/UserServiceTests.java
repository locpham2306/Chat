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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
    void searchUsersTrimsKeywordExcludesCurrentUserAndMapsPage() {
        var currentUserId = UUID.randomUUID();
        var first = user(UUID.randomUUID(), "alpha", "Alpha");
        var second = user(UUID.randomUUID(), "beta", "Beta");
        var repositoryPage = new PageImpl<>(List.of(first, second), PageRequest.of(1, 2), 5);
        when(users.searchPublicProfiles(eq(currentUserId), eq("needle"), any(Pageable.class)))
                .thenReturn(repositoryPage);

        var response = service.searchUsers(currentUserId, "  needle  ", 1, 2);

        assertThat(response.data()).extracting(user -> user.username())
                .containsExactly("alpha", "beta");
        assertThat(response.data()).allMatch(user -> user.status().name().equals("OFFLINE"));
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(5);

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(users).searchPublicProfiles(eq(currentUserId), eq("needle"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageable.getValue().getSort().getOrderFor("username")).isNotNull();
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

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
        return user(id, "an", "An");
    }

    private UserEntity user(UUID id, String username, String fullName) {
        var user = new UserEntity(username, "hash", fullName);
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
