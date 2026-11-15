package com.chat.modules.conversation.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.dto.response.PageResponse;
import com.chat.exception.BusinessException;
import com.chat.modules.conversation.dto.request.AddConversationMembersRequest;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.request.UpdateConversationRequest;
import com.chat.modules.conversation.dto.response.AddConversationMembersResponse;
import com.chat.modules.conversation.dto.response.ConversationDetailResponse;
import com.chat.modules.conversation.dto.response.ConversationListItemResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.dto.response.ConversationPeerResponse;
import com.chat.modules.conversation.dto.response.DirectConversationLookupResponse;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMemberMapper;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import com.chat.modules.conversation.service.ConversationMemberService;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.mapper.MessageMapper;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.message.service.MessageResponseLoader;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserPresence;
import com.chat.utils.PageableUtils;
import com.chat.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final FileRepository files;
    private final MessageRepository messages;
    private final ConversationMapper conversationMapper;
    private final MessageResponseLoader messageResponses;
    private final UserPresence presence;
    private final Clock clock;
    private ConversationMemberService compatibilityMemberService;

    /**
     * Keeps source compatibility for callers that still construct the old combined service.
     */
    public ConversationServiceImpl(
            ConversationRepository conversations,
            ConversationMemberRepository members,
            UserRepository users,
            FileRepository files,
            MessageRepository messages,
            MessageAttachmentRepository attachments,
            ConversationMapper conversationMapper,
            ConversationMemberMapper memberMapper,
            MessageMapper messageMapper,
            UserMapper userMapper,
            Clock clock) {
        this.conversations = conversations;
        this.members = members;
        this.users = users;
        this.files = files;
        this.messages = messages;
        this.conversationMapper = conversationMapper;
        this.messageResponses = new MessageResponseLoader(attachments, files, users, messageMapper);
        this.presence = ignored -> UserStatusEnum.OFFLINE;
        this.clock = clock;
        this.compatibilityMemberService = new ConversationMemberServiceImpl(
                conversations, members, users, memberMapper, userMapper, presence, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectConversationLookupResponse findDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw BusinessException.badRequest("conversation.recipientIsCurrentUser");
        }
        UserEntity recipient = users.findById(recipientId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        DirectConversationResponse conversation = conversations.findDirectBetweenUsers(
                        ConversationTypeEnum.DIRECT, currentUserId, recipientId)
                .map(conversationMapper::toDirectResponse)
                .orElse(null);
        return new DirectConversationLookupResponse(
                conversation, conversationMapper.toPeerResponse(recipient, presence.status(recipientId)));
    }

    @Override
    @Transactional
    public GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request) {
        LinkedHashSet<UUID> memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.remove(currentUserId);
        if (memberIds.isEmpty()) {
            throw BusinessException.badRequest("conversation.groupRequiresMember");
        }
        if (users.countByIdIn(memberIds) != memberIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        validateGroupAvatar(currentUserId, request.avatarFileId());

        ConversationEntity conversation = conversations.saveAndFlush(ConversationEntity.group(
                currentUserId, StringUtils.trim(request.name()), request.avatarFileId()));
        Instant joinedAt = clock.instant();
        ArrayList<ConversationMemberEntity> memberships =
                new ArrayList<>(memberIds.size() + 1);
        memberships.add(new ConversationMemberEntity(
                conversation.getId(), currentUserId, MemberRoleEnum.OWNER, joinedAt));
        for (UUID memberId : memberIds) {
            memberships.add(new ConversationMemberEntity(
                    conversation.getId(), memberId, MemberRoleEnum.MEMBER, joinedAt));
        }
        members.saveAllAndFlush(memberships);

        return conversationMapper.toGroupResponse(conversation, memberships.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<ConversationListItemResponse>> getConversations(
            UUID currentUserId, Integer page, Integer size) {
        Page<ConversationEntity> conversationsPage = conversations.findActiveForUser(
                currentUserId, PageableUtils.of(page, size));
        if (conversationsPage.isEmpty()) {
            return PageResponse.of(List.of(), conversationsPage.getTotalPages(), conversationsPage.getTotalElements());
        }

        List<UUID> conversationIds = conversationsPage.getContent().stream()
                .map(ConversationEntity::getId)
                .toList();
        Map<UUID, UserEntity> peersById = loadDirectPeers(
                conversationsPage.getContent(), currentUserId);
        Map<UUID, Long> memberCounts = toCountMap(
                members.countActiveMembersByConversationIds(conversationIds));
        Map<UUID, Long> unreadCounts = toCountMap(
                messages.countUnreadByConversationIds(currentUserId, conversationIds));

        Map<UUID, MessageEntity> latestMessages = new HashMap<>();
        for (MessageEntity message : messages.findLatestByConversationIds(conversationIds)) {
            latestMessages.putIfAbsent(message.getConversationId(), message);
        }
        Map<UUID, MessageResponse> lastMessageResponses = messageResponses.load(latestMessages.values());

        List<ConversationListItemResponse> data = new ArrayList<>();
        for (ConversationEntity conversation : conversationsPage.getContent()) {
            UUID conversationId = conversation.getId();
            MessageEntity lastMessage = latestMessages.get(conversationId);
            MessageResponse lastMessageResponse = lastMessage == null
                    ? null
                    : lastMessageResponses.get(lastMessage.getId());

            ConversationListItemResponse item = conversationMapper.toListItemResponse(
                    conversation,
                    memberCounts.getOrDefault(conversationId, 0L),
                    peerFor(conversation, currentUserId, peersById),
                    lastMessageResponse,
                    unreadCounts.getOrDefault(conversationId, 0L));
            data.add(item);
        }
        return PageResponse.of(data, conversationsPage.getTotalPages(), conversationsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID currentUserId, UUID conversationId) {
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        long memberCount = members.countByConversationIdAndLeftAtIsNull(conversationId);
        ConversationPeerResponse peer = conversation.getType() == ConversationTypeEnum.DIRECT
                ? mapPeer(users.findById(directPeerId(conversation, currentUserId))
                        .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND)))
                : null;
        return conversationMapper.toDetailResponse(conversation, memberCount, peer);
    }

    @Override
    @Transactional
    public ConversationDetailResponse updateConversation(
            UUID currentUserId, UUID conversationId, UpdateConversationRequest request) {
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict("conversation.directCannotBeUpdated");
        }
        ConversationMemberEntity membership = members.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        if (membership.getRole() != MemberRoleEnum.OWNER
                || !currentUserId.equals(conversation.getOwnerId())) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        validateGroupAvatar(currentUserId, request.avatarFileId());

        conversation.updateGroup(StringUtils.trim(request.name()), request.avatarFileId());
        long memberCount = members.countByConversationIdAndLeftAtIsNull(conversationId);
        return conversationMapper.toDetailResponse(conversation, memberCount, null);
    }

    public PageResponse<List<ConversationMemberResponse>> getConversationMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size) {
        return compatibilityMembers().getMembers(currentUserId, conversationId, page, size);
    }

    public PageResponse<List<UserResponse>> getAvailableGroupMembers(
            UUID currentUserId, UUID conversationId, String keyword, Integer page, Integer size) {
        return compatibilityMembers().getAvailableMembers(
                currentUserId, conversationId, keyword, page, size);
    }

    public AddConversationMembersResponse addConversationMembers(
            UUID currentUserId, UUID conversationId, AddConversationMembersRequest request) {
        return compatibilityMembers().addMembers(currentUserId, conversationId, request);
    }

    public void removeConversationMember(UUID currentUserId, UUID conversationId, UUID userId) {
        compatibilityMembers().removeMember(currentUserId, conversationId, userId);
    }

    private Map<UUID, UserEntity> loadDirectPeers(
            List<ConversationEntity> conversationEntities, UUID currentUserId) {
        LinkedHashSet<UUID> peerIds = new LinkedHashSet<>();
        for (ConversationEntity conversation : conversationEntities) {
            if (conversation.getType() == ConversationTypeEnum.DIRECT) {
                peerIds.add(directPeerId(conversation, currentUserId));
            }
        }
        if (peerIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UserEntity> peersById = new HashMap<>();
        for (UserEntity user : users.findAllById(peerIds)) {
            peersById.put(user.getId(), user);
        }
        return peersById;
    }

    private ConversationMemberService compatibilityMembers() {
        if (compatibilityMemberService == null) {
            throw new IllegalStateException("Compatibility member service is unavailable");
        }
        return compatibilityMemberService;
    }

    private Map<UUID, Long> toCountMap(List<ConversationCountProjection> projections) {
        Map<UUID, Long> counts = new HashMap<>();
        for (ConversationCountProjection projection : projections) {
            counts.put(projection.getConversationId(), projection.getTotal());
        }
        return counts;
    }

    private ConversationPeerResponse peerFor(
            ConversationEntity conversation,
            UUID currentUserId,
            Map<UUID, UserEntity> peersById) {
        if (conversation.getType() != ConversationTypeEnum.DIRECT) {
            return null;
        }
        UUID peerId = directPeerId(conversation, currentUserId);
        return mapPeer(Optional.ofNullable(peersById.get(peerId))
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND)));
    }

    private ConversationPeerResponse mapPeer(UserEntity user) {
        return conversationMapper.toPeerResponse(user, presence.status(user.getId()));
    }

    private UUID directPeerId(ConversationEntity conversation, UUID currentUserId) {
        if (currentUserId.equals(conversation.getDirectUserLowId())) {
            return conversation.getDirectUserHighId();
        }
        if (currentUserId.equals(conversation.getDirectUserHighId())) {
            return conversation.getDirectUserLowId();
        }
        throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
    }

    private void validateGroupAvatar(UUID currentUserId, UUID avatarFileId) {
        if (avatarFileId == null) {
            return;
        }
        FileEntity avatar = files.findById(avatarFileId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        boolean allowed = currentUserId.equals(avatar.getUploaderId())
                && avatar.getConversationId() == null
                && StringUtils.isImageContentType(avatar.getContentType());
        if (!allowed) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }
}
