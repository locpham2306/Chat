package com.chat.modules.conversation;

import com.chat.exception.BusinessException;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.impl.ConversationServiceImpl;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConversationServiceTests {
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final ConversationMemberRepository members = mock(ConversationMemberRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FileRepository files = mock(FileRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T08:00:00Z"), ZoneOffset.UTC);
    private final ConversationServiceImpl service = new ConversationServiceImpl(
            conversations, members, users, files, new ConversationMapper(), clock);

    @Test
    void directLookupReturnsExistingConversationWithoutCreatingData() {
        var currentUserId = UUID.randomUUID();
        var recipientId = UUID.randomUUID();
        var conversation = ConversationEntity.direct(currentUserId, recipientId);
        var conversationId = UUID.randomUUID();
        var createdAt = Instant.parse("2026-10-20T08:00:00Z");
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", createdAt);
        when(users.existsById(recipientId)).thenReturn(true);
        when(conversations.findDirectBetweenUsers(
                ConversationTypeEnum.DIRECT, currentUserId, recipientId))
                .thenReturn(Optional.of(conversation));

        var result = service.findDirectConversation(currentUserId, recipientId).orElseThrow();

        assertThat(result.id()).isEqualTo(conversationId);
        assertThat(result.type()).isEqualTo(ConversationTypeEnum.DIRECT);
        assertThat(result.memberIds()).containsExactlyInAnyOrder(currentUserId, recipientId);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        verify(conversations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void directLookupReturnsEmptyWithoutCreatingConversation() {
        var currentUserId = UUID.randomUUID();
        var recipientId = UUID.randomUUID();
        when(users.existsById(recipientId)).thenReturn(true);
        when(conversations.findDirectBetweenUsers(
                ConversationTypeEnum.DIRECT, currentUserId, recipientId))
                .thenReturn(Optional.empty());

        assertThat(service.findDirectConversation(currentUserId, recipientId)).isEmpty();

        verify(conversations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void directLookupRejectsCurrentUserAndUnknownRecipient() {
        var currentUserId = UUID.randomUUID();
        assertStatus(() -> service.findDirectConversation(currentUserId, currentUserId), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(conversations, users);

        var recipientId = UUID.randomUUID();
        when(users.existsById(recipientId)).thenReturn(false);
        assertStatus(() -> service.findDirectConversation(currentUserId, recipientId), HttpStatus.NOT_FOUND);
        verifyNoInteractions(conversations);
    }

    @Test
    void createGroupTrimsNameDeduplicatesMembersAndAssignsRoles() {
        var currentUserId = UUID.randomUUID();
        var firstMemberId = UUID.randomUUID();
        var secondMemberId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        var avatar = new FileEntity(currentUserId, "group.png", "storage-key", "image/png", 10);
        ReflectionTestUtils.setField(avatar, "id", avatarId);
        var conversationId = UUID.randomUUID();
        when(users.countByIdIn(anyCollection())).thenReturn(2L);
        when(files.findById(avatarId)).thenReturn(Optional.of(avatar));
        when(conversations.saveAndFlush(any(ConversationEntity.class))).thenAnswer(invocation -> {
            var conversation = invocation.getArgument(0, ConversationEntity.class);
            ReflectionTestUtils.setField(conversation, "id", conversationId);
            ReflectionTestUtils.setField(conversation, "createdAt", clock.instant());
            return conversation;
        });

        var response = service.createGroup(currentUserId, new CreateGroupRequest(
                "  Nhóm BTL  ",
                List.of(firstMemberId, currentUserId, firstMemberId, secondMemberId),
                avatarId));

        assertThat(response.id()).isEqualTo(conversationId);
        assertThat(response.type()).isEqualTo(ConversationTypeEnum.GROUP);
        assertThat(response.name()).isEqualTo("Nhóm BTL");
        assertThat(response.avatarFileId()).isEqualTo(avatarId);
        assertThat(response.ownerId()).isEqualTo(currentUserId);
        assertThat(response.memberCount()).isEqualTo(3);
        assertThat(response.createdAt()).isEqualTo(clock.instant());

        @SuppressWarnings("unchecked")
        var memberships = (ArgumentCaptor<List<ConversationMemberEntity>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        verify(members).saveAllAndFlush(memberships.capture());
        assertThat(memberships.getValue()).hasSize(3);
        assertThat(memberships.getValue())
                .anySatisfy(member -> {
                    assertThat(member.getUserId()).isEqualTo(currentUserId);
                    assertThat(member.getRole()).isEqualTo(MemberRoleEnum.OWNER);
                    assertThat(member.getJoinedAt()).isEqualTo(clock.instant());
                })
                .filteredOn(member -> member.getRole() == MemberRoleEnum.MEMBER)
                .extracting(ConversationMemberEntity::getUserId)
                .containsExactlyInAnyOrder(firstMemberId, secondMemberId);
    }

    @Test
    void createGroupRejectsMissingOtherMemberAndUnknownUser() {
        var currentUserId = UUID.randomUUID();
        assertStatus(() -> service.createGroup(currentUserId,
                new CreateGroupRequest("Nhóm", List.of(currentUserId), null)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(conversations, members, files);

        var firstMemberId = UUID.randomUUID();
        var missingMemberId = UUID.randomUUID();
        when(users.countByIdIn(anyCollection())).thenReturn(1L);
        assertStatus(() -> service.createGroup(currentUserId,
                new CreateGroupRequest("Nhóm", List.of(firstMemberId, missingMemberId), null)),
                HttpStatus.NOT_FOUND);
        verifyNoInteractions(conversations, members, files);
    }

    @Test
    void createGroupRejectsMissingOrUnauthorizedAvatar() {
        var currentUserId = UUID.randomUUID();
        var memberId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        when(users.countByIdIn(anyCollection())).thenReturn(1L);

        assertStatus(() -> service.createGroup(currentUserId,
                new CreateGroupRequest("Nhóm", List.of(memberId), avatarId)), HttpStatus.NOT_FOUND);

        var foreignAvatar = new FileEntity(
                UUID.randomUUID(), "group.png", "storage-key", "image/png", 10);
        when(files.findById(avatarId)).thenReturn(Optional.of(foreignAvatar));
        assertStatus(() -> service.createGroup(currentUserId,
                new CreateGroupRequest("Nhóm", List.of(memberId), avatarId)), HttpStatus.FORBIDDEN);
        verifyNoInteractions(conversations, members);
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
