package com.chat.modules.conversation;

import com.chat.exception.BusinessException;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.mapper.ConversationMemberMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import com.chat.modules.conversation.service.impl.ConversationServiceImpl;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.entity.MessageAttachmentEntity;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.mapper.MessageMapper;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private final MessageRepository messages = mock(MessageRepository.class);
    private final MessageAttachmentRepository attachments = mock(MessageAttachmentRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T08:00:00Z"), ZoneOffset.UTC);
    private final ConversationServiceImpl service = new ConversationServiceImpl(
            conversations, members, users, files, messages, attachments,
            new ConversationMapper(), new ConversationMemberMapper(new UserMapper()),
            new MessageMapper(), new UserMapper(), clock);

    @Test
    void directLookupReturnsExistingConversationWithoutCreatingData() {
        var currentUserId = UUID.randomUUID();
        var recipientId = UUID.randomUUID();
        var recipient = new UserEntity("recipient", "hash", "Người nhận");
        ReflectionTestUtils.setField(recipient, "id", recipientId);
        var conversation = ConversationEntity.direct(currentUserId, recipientId);
        var conversationId = UUID.randomUUID();
        var createdAt = Instant.parse("2026-10-20T08:00:00Z");
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", createdAt);
        when(users.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(conversations.findDirectBetweenUsers(
                ConversationTypeEnum.DIRECT, currentUserId, recipientId))
                .thenReturn(Optional.of(conversation));

        var result = service.findDirectConversation(currentUserId, recipientId);

        assertThat(result.conversation().id()).isEqualTo(conversationId);
        assertThat(result.conversation().type()).isEqualTo(ConversationTypeEnum.DIRECT);
        assertThat(result.conversation().createdAt()).isEqualTo(createdAt);
        assertThat(result.peer().id()).isEqualTo(recipientId);
        assertThat(result.peer().username()).isEqualTo("recipient");
        assertThat(result.peer().fullName()).isEqualTo("Người nhận");
        assertThat(result.peer().status()).isEqualTo(UserStatusEnum.OFFLINE);
        verify(conversations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void directLookupReturnsEmptyWithoutCreatingConversation() {
        var currentUserId = UUID.randomUUID();
        var recipientId = UUID.randomUUID();
        var recipient = new UserEntity("recipient", "hash", "Người nhận");
        ReflectionTestUtils.setField(recipient, "id", recipientId);
        when(users.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(conversations.findDirectBetweenUsers(
                ConversationTypeEnum.DIRECT, currentUserId, recipientId))
                .thenReturn(Optional.empty());

        var result = service.findDirectConversation(currentUserId, recipientId);

        assertThat(result.conversation()).isNull();
        assertThat(result.peer().id()).isEqualTo(recipientId);
        assertThat(result.peer().username()).isEqualTo("recipient");

        verify(conversations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void directLookupRejectsCurrentUserAndUnknownRecipient() {
        var currentUserId = UUID.randomUUID();
        assertStatus(() -> service.findDirectConversation(currentUserId, currentUserId), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(conversations, users);

        var recipientId = UUID.randomUUID();
        when(users.findById(recipientId)).thenReturn(Optional.empty());
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

    @Test
    void getConversationsBuildsPageWithLatestMessageAttachmentsAndUnreadCount() {
        var currentUserId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();
        var group = ConversationEntity.group(currentUserId, "Nhóm", null);
        var direct = ConversationEntity.direct(currentUserId, otherUserId);
        var groupId = UUID.randomUUID();
        var directId = UUID.randomUUID();
        ReflectionTestUtils.setField(group, "id", groupId);
        ReflectionTestUtils.setField(group, "createdAt", clock.instant());
        ReflectionTestUtils.setField(direct, "id", directId);
        ReflectionTestUtils.setField(direct, "createdAt", clock.instant());
        when(conversations.findActiveForUser(eq(currentUserId), any()))
                .thenReturn(new PageImpl<>(List.of(group, direct)));
        var groupMemberCount = count(groupId, 3);
        var directMemberCount = count(directId, 2);
        var groupUnreadCount = count(groupId, 1);
        when(members.countActiveMembersByConversationIds(anyCollection()))
                .thenReturn(List.of(groupMemberCount, directMemberCount));
        when(messages.countUnreadByConversationIds(eq(currentUserId), anyCollection()))
                .thenReturn(List.of(groupUnreadCount));

        var message = new MessageEntity(groupId, otherUserId, "client-1", "Xin chào");
        var messageId = UUID.randomUUID();
        ReflectionTestUtils.setField(message, "id", messageId);
        ReflectionTestUtils.setField(message, "createdAt", clock.instant());
        when(messages.findLatestByConversationIds(anyCollection())).thenReturn(List.of(message));
        var sender = new UserEntity("sender", "hash", "Người gửi");
        ReflectionTestUtils.setField(sender, "id", otherUserId);
        when(users.findAllById(anyCollection())).thenReturn(List.of(sender));
        var file = new FileEntity(otherUserId, "hello.txt", "storage-key", "text/plain", 5);
        var fileId = UUID.randomUUID();
        ReflectionTestUtils.setField(file, "id", fileId);
        var attachment = new MessageAttachmentEntity(messageId, fileId, 0);
        when(attachments.findByMessageIdInOrderByMessageIdAscPositionAsc(anyCollection()))
                .thenReturn(List.of(attachment));
        when(files.findAllById(anyCollection())).thenReturn(List.of(file));

        var result = service.getConversations(currentUserId, 0, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.data()).hasSize(2);
        var groupResult = result.data().getFirst();
        assertThat(groupResult.id()).isEqualTo(groupId);
        assertThat(groupResult.type()).isEqualTo(ConversationTypeEnum.GROUP);
        assertThat(groupResult.peer()).isNull();
        assertThat(groupResult.memberCount()).isEqualTo(3);
        assertThat(groupResult.unreadCount()).isEqualTo(1);
        assertThat(groupResult.lastMessage().id()).isEqualTo(messageId);
        assertThat(groupResult.lastMessage().sender().id()).isEqualTo(otherUserId);
        assertThat(groupResult.lastMessage().sender().fullName()).isEqualTo("Người gửi");
        assertThat(groupResult.lastMessage().attachments()).singleElement().satisfies(fileResponse -> {
            assertThat(fileResponse.id()).isEqualTo(fileId);
            assertThat(fileResponse.downloadUrl()).isEqualTo("/api/files/" + fileId);
        });
        var directResult = result.data().get(1);
        assertThat(directResult.type()).isEqualTo(ConversationTypeEnum.DIRECT);
        assertThat(directResult.peer().id()).isEqualTo(otherUserId);
        assertThat(directResult.peer().username()).isEqualTo("sender");
        assertThat(directResult.peer().fullName()).isEqualTo("Người gửi");
        assertThat(directResult.peer().status()).isEqualTo(UserStatusEnum.OFFLINE);
        assertThat(directResult.memberCount()).isEqualTo(2);
        assertThat(directResult.lastMessage()).isNull();
        assertThat(directResult.unreadCount()).isZero();
    }

    @Test
    void getConversationReturnsDetailForActiveMember() {
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var conversation = ConversationEntity.group(currentUserId, "Nhóm BTL", null);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", clock.instant());
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(true);
        when(members.countByConversationIdAndLeftAtIsNull(conversationId)).thenReturn(3L);

        var result = service.getConversation(currentUserId, conversationId);

        assertThat(result.id()).isEqualTo(conversationId);
        assertThat(result.type()).isEqualTo(ConversationTypeEnum.GROUP);
        assertThat(result.name()).isEqualTo("Nhóm BTL");
        assertThat(result.ownerId()).isEqualTo(currentUserId);
        assertThat(result.memberCount()).isEqualTo(3);
        assertThat(result.createdAt()).isEqualTo(clock.instant());
        assertThat(result.peer()).isNull();
    }

    @Test
    void getDirectConversationDetailIncludesPeer() {
        var currentUserId = UUID.randomUUID();
        var peerId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var conversation = ConversationEntity.direct(currentUserId, peerId);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", clock.instant());
        var peer = new UserEntity("peer", "hash", "Người đối thoại");
        ReflectionTestUtils.setField(peer, "id", peerId);
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(true);
        when(members.countByConversationIdAndLeftAtIsNull(conversationId)).thenReturn(2L);
        when(users.findById(peerId)).thenReturn(Optional.of(peer));

        var result = service.getConversation(currentUserId, conversationId);

        assertThat(result.type()).isEqualTo(ConversationTypeEnum.DIRECT);
        assertThat(result.peer().id()).isEqualTo(peerId);
        assertThat(result.peer().username()).isEqualTo("peer");
        assertThat(result.peer().fullName()).isEqualTo("Người đối thoại");
        assertThat(result.peer().status()).isEqualTo(UserStatusEnum.OFFLINE);
    }

    @Test
    void getConversationRejectsUnknownConversationAndNonMember() {
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        when(conversations.findById(conversationId)).thenReturn(Optional.empty());

        assertStatus(() -> service.getConversation(currentUserId, conversationId), HttpStatus.NOT_FOUND);
        verifyNoInteractions(members);

        var conversation = ConversationEntity.group(UUID.randomUUID(), "Nhóm", null);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(false);

        assertStatus(() -> service.getConversation(currentUserId, conversationId), HttpStatus.FORBIDDEN);
        verify(members, never()).countByConversationIdAndLeftAtIsNull(conversationId);
    }

    @Test
    void updateConversationAllowsOwnerAndTrimsName() {
        var ownerId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        var conversation = ConversationEntity.group(ownerId, "Tên cũ", null);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", clock.instant());
        var ownerMembership = new ConversationMemberEntity(
                conversationId, ownerId, MemberRoleEnum.OWNER, clock.instant());
        var avatar = new FileEntity(ownerId, "group.png", "group-avatar", "image/png", 10);
        ReflectionTestUtils.setField(avatar, "id", avatarId);
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, ownerId))
                .thenReturn(Optional.of(ownerMembership));
        when(files.findById(avatarId)).thenReturn(Optional.of(avatar));
        when(members.countByConversationIdAndLeftAtIsNull(conversationId)).thenReturn(3L);

        var result = service.updateConversation(ownerId, conversationId,
                new UpdateConversationRequest("  Tên mới  ", avatarId));

        assertThat(result.name()).isEqualTo("Tên mới");
        assertThat(result.avatarFileId()).isEqualTo(avatarId);
        assertThat(result.memberCount()).isEqualTo(3);
        assertThat(conversation.getName()).isEqualTo("Tên mới");
        assertThat(conversation.getAvatarFileId()).isEqualTo(avatarId);
    }

    @Test
    void updateConversationRejectsUnknownConversationAndDirectConversation() {
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var request = new UpdateConversationRequest("Tên mới", null);
        when(conversations.findById(conversationId)).thenReturn(Optional.empty());

        assertStatus(() -> service.updateConversation(currentUserId, conversationId, request),
                HttpStatus.NOT_FOUND);
        verifyNoInteractions(members, files);

        var direct = ConversationEntity.direct(currentUserId, UUID.randomUUID());
        when(conversations.findById(conversationId)).thenReturn(Optional.of(direct));

        assertStatus(() -> service.updateConversation(currentUserId, conversationId, request),
                HttpStatus.CONFLICT);
        verifyNoInteractions(members, files);
    }

    @Test
    void updateConversationRejectsNonOwnerAndInvalidAvatar() {
        var ownerId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var avatarId = UUID.randomUUID();
        var conversation = ConversationEntity.group(ownerId, "Tên cũ", null);
        var member = new ConversationMemberEntity(
                conversationId, currentUserId, MemberRoleEnum.MEMBER, clock.instant());
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(Optional.of(member));

        assertStatus(() -> service.updateConversation(currentUserId, conversationId,
                new UpdateConversationRequest("Tên mới", avatarId)), HttpStatus.FORBIDDEN);
        verifyNoInteractions(files);

        var ownerMembership = new ConversationMemberEntity(
                conversationId, ownerId, MemberRoleEnum.OWNER, clock.instant());
        when(members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, ownerId))
                .thenReturn(Optional.of(ownerMembership));
        when(files.findById(avatarId)).thenReturn(Optional.empty());
        assertStatus(() -> service.updateConversation(ownerId, conversationId,
                new UpdateConversationRequest("Tên mới", avatarId)), HttpStatus.NOT_FOUND);

        var foreignAvatar = new FileEntity(
                currentUserId, "group.png", "foreign-avatar", "image/png", 10);
        when(files.findById(avatarId)).thenReturn(Optional.of(foreignAvatar));
        assertStatus(() -> service.updateConversation(ownerId, conversationId,
                new UpdateConversationRequest("Tên mới", avatarId)), HttpStatus.FORBIDDEN);
        assertThat(conversation.getName()).isEqualTo("Tên cũ");
        assertThat(conversation.getAvatarFileId()).isNull();
    }

    @Test
    void getConversationMembersReturnsActiveMembersWithPublicProfiles() {
        var conversationId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var memberId = UUID.randomUUID();
        var ownerJoinedAt = clock.instant().minusSeconds(60);
        var memberJoinedAt = clock.instant();
        var ownerMembership = new ConversationMemberEntity(
                conversationId, ownerId, MemberRoleEnum.OWNER, ownerJoinedAt);
        var memberMembership = new ConversationMemberEntity(
                conversationId, memberId, MemberRoleEnum.MEMBER, memberJoinedAt);
        var owner = new UserEntity("owner", "hash", "Chủ nhóm");
        var member = new UserEntity("member", "hash", "Thành viên");
        ReflectionTestUtils.setField(owner, "id", ownerId);
        ReflectionTestUtils.setField(member, "id", memberId);
        when(conversations.existsById(conversationId)).thenReturn(true);
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, memberId))
                .thenReturn(true);
        when(members.findByConversationIdAndLeftAtIsNull(eq(conversationId), any()))
                .thenReturn(new PageImpl<>(List.of(ownerMembership, memberMembership)));
        when(users.findAllById(anyCollection())).thenReturn(List.of(owner, member));

        var result = service.getConversationMembers(memberId, conversationId, 0, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data().getFirst().user().id()).isEqualTo(ownerId);
        assertThat(result.data().getFirst().user().status()).isEqualTo(UserStatusEnum.OFFLINE);
        assertThat(result.data().getFirst().role()).isEqualTo(MemberRoleEnum.OWNER);
        assertThat(result.data().getFirst().joinedAt()).isEqualTo(ownerJoinedAt);
        assertThat(result.data().get(1).user().id()).isEqualTo(memberId);
        assertThat(result.data().get(1).role()).isEqualTo(MemberRoleEnum.MEMBER);
        assertThat(result.data().get(1).joinedAt()).isEqualTo(memberJoinedAt);
    }

    @Test
    void getConversationMembersRejectsUnknownConversationAndNonMember() {
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        when(conversations.existsById(conversationId)).thenReturn(false);

        assertStatus(() -> service.getConversationMembers(currentUserId, conversationId, 0, 20),
                HttpStatus.NOT_FOUND);
        verifyNoInteractions(members);

        when(conversations.existsById(conversationId)).thenReturn(true);
        when(members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(false);
        assertStatus(() -> service.getConversationMembers(currentUserId, conversationId, 0, 20),
                HttpStatus.FORBIDDEN);
        verify(members, never()).findByConversationIdAndLeftAtIsNull(eq(conversationId), any());
        verifyNoInteractions(users);
    }

    @Test
    void getAvailableGroupMembersReturnsOnlyRepositoryCandidates() {
        var ownerId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var candidateId = UUID.randomUUID();
        var conversation = ConversationEntity.group(ownerId, "Nhóm", null);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        var ownerMembership = new ConversationMemberEntity(
                conversationId, ownerId, MemberRoleEnum.OWNER, clock.instant());
        var candidate = new UserEntity("candidate", "hash", "Ứng viên");
        ReflectionTestUtils.setField(candidate, "id", candidateId);
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, ownerId))
                .thenReturn(Optional.of(ownerMembership));
        when(users.searchAvailableGroupMembers(eq(ownerId), eq(conversationId), eq("ứng"), any()))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        var result = service.getAvailableGroupMembers(ownerId, conversationId, "  ứng  ", 0, 20);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.data()).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(candidateId);
            assertThat(user.username()).isEqualTo("candidate");
            assertThat(user.fullName()).isEqualTo("Ứng viên");
            assertThat(user.status()).isEqualTo(UserStatusEnum.OFFLINE);
        });
    }

    @Test
    void getAvailableGroupMembersRequiresGroupOwner() {
        var currentUserId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        when(conversations.findById(conversationId)).thenReturn(Optional.empty());
        assertStatus(() -> service.getAvailableGroupMembers(
                currentUserId, conversationId, "", 0, 20), HttpStatus.NOT_FOUND);

        var direct = ConversationEntity.direct(currentUserId, UUID.randomUUID());
        ReflectionTestUtils.setField(direct, "id", conversationId);
        when(conversations.findById(conversationId)).thenReturn(Optional.of(direct));
        assertStatus(() -> service.getAvailableGroupMembers(
                currentUserId, conversationId, "", 0, 20), HttpStatus.CONFLICT);

        var ownerId = UUID.randomUUID();
        var group = ConversationEntity.group(ownerId, "Nhóm", null);
        ReflectionTestUtils.setField(group, "id", conversationId);
        var member = new ConversationMemberEntity(
                conversationId, currentUserId, MemberRoleEnum.MEMBER, clock.instant());
        when(conversations.findById(conversationId)).thenReturn(Optional.of(group));
        when(members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId))
                .thenReturn(Optional.of(member));
        assertStatus(() -> service.getAvailableGroupMembers(
                currentUserId, conversationId, "", 0, 20), HttpStatus.FORBIDDEN);
        verify(users, never()).searchAvailableGroupMembers(any(), any(), any(), any());
    }

    private ConversationCountProjection count(UUID conversationId, long total) {
        var projection = mock(ConversationCountProjection.class);
        when(projection.getConversationId()).thenReturn(conversationId);
        when(projection.getTotal()).thenReturn(total);
        return projection;
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
