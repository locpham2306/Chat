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
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.mapper.ConversationMemberMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.file.entity.FileEntity;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.message.dto.response.MessageResponse;
import com.chat.modules.message.entity.MessageEntity;
import com.chat.modules.message.mapper.MessageMapper;
import com.chat.modules.message.repository.MessageAttachmentRepository;
import com.chat.modules.message.repository.MessageRepository;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserPresence;
import com.chat.utils.PageableUtils;
import com.chat.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final FileRepository files;
    private final MessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserPresence presence;
    private final Clock clock;

    @Autowired
    public ConversationServiceImpl(
            ConversationRepository conversations,
            ConversationMemberRepository members,
            UserRepository users,
            FileRepository files,
            MessageRepository messages,
            MessageAttachmentRepository attachments,
            ConversationMapper conversationMapper,
            ConversationMemberMapper conversationMemberMapper,
            MessageMapper messageMapper,
            UserMapper userMapper,
            UserPresence presence,
            Clock clock) {
        this.conversations = conversations;
        this.members = members;
        this.users = users;
        this.files = files;
        this.messages = messages;
        this.attachments = attachments;
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.presence = presence;
        this.clock = clock;
    }

    public ConversationServiceImpl(
            ConversationRepository conversations,
            ConversationMemberRepository members,
            UserRepository users,
            FileRepository files,
            MessageRepository messages,
            MessageAttachmentRepository attachments,
            ConversationMapper conversationMapper,
            ConversationMemberMapper conversationMemberMapper,
            MessageMapper messageMapper,
            UserMapper userMapper,
            Clock clock) {
        this(conversations, members, users, files, messages, attachments,
                conversationMapper, conversationMemberMapper, messageMapper, userMapper,
                ignored -> UserStatusEnum.OFFLINE, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectConversationLookupResponse findDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw BusinessException.badRequest("conversation.recipientIsCurrentUser");
        }
        var recipient = users.findById(recipientId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        var conversation = conversations.findDirectBetweenUsers(
                        ConversationTypeEnum.DIRECT, currentUserId, recipientId)
                .map(conversationMapper::toDirectResponse)
                .orElse(null);
        return new DirectConversationLookupResponse(
                conversation, conversationMapper.toPeerResponse(recipient, presence.status(recipientId)));
    }

    @Override
    @Transactional
    public GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request) {
        var memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.remove(currentUserId);
        if (memberIds.isEmpty()) {
            throw BusinessException.badRequest("conversation.groupRequiresMember");
        }
        if (users.countByIdIn(memberIds) != memberIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        validateGroupAvatar(currentUserId, request.avatarFileId());

        var conversation = conversations.saveAndFlush(ConversationEntity.group(
                currentUserId, StringUtils.trim(request.name()), request.avatarFileId()));
        var joinedAt = clock.instant();
        var memberships = new ArrayList<ConversationMemberEntity>(memberIds.size() + 1);
        memberships.add(new ConversationMemberEntity(
                conversation.getId(), currentUserId, MemberRoleEnum.OWNER, joinedAt));
        memberIds.forEach(memberId -> memberships.add(new ConversationMemberEntity(
                conversation.getId(), memberId, MemberRoleEnum.MEMBER, joinedAt)));
        members.saveAllAndFlush(memberships);

        return conversationMapper.toGroupResponse(conversation, memberships.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<ConversationListItemResponse>> getConversations(
            UUID currentUserId, Integer page, Integer size) {
        var conversationsPage = conversations.findActiveForUser(
                currentUserId, PageableUtils.of(page, size));
        if (conversationsPage.isEmpty()) {
            return PageResponse.of(List.of(), conversationsPage.getTotalPages(), conversationsPage.getTotalElements());
        }

        var conversationIds = conversationsPage.getContent().stream()
                .map(ConversationEntity::getId)
                .toList();
        var peersById = loadDirectPeers(conversationsPage.getContent(), currentUserId);
        Map<UUID, Long> memberCounts = members.countActiveMembersByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(
                        projection -> projection.getConversationId(),
                        projection -> projection.getTotal()));
        Map<UUID, Long> unreadCounts = messages
                .countUnreadByConversationIds(currentUserId, conversationIds).stream()
                .collect(Collectors.toMap(
                        projection -> projection.getConversationId(),
                        projection -> projection.getTotal()));

        Map<UUID, MessageEntity> latestMessages = new HashMap<>();
        messages.findLatestByConversationIds(conversationIds)
                .forEach(message -> latestMessages.putIfAbsent(message.getConversationId(), message));
        var lastMessageResponses = mapMessagesWithAttachments(latestMessages.values());

        var data = conversationsPage.getContent().stream()
                .map(conversation -> conversationMapper.toListItemResponse(
                        conversation,
                        memberCounts.getOrDefault(conversation.getId(), 0L),
                        peerFor(conversation, currentUserId, peersById),
                        Optional.ofNullable(latestMessages.get(conversation.getId()))
                                .map(message -> lastMessageResponses.get(message.getId()))
                                .orElse(null),
                        unreadCounts.getOrDefault(conversation.getId(), 0L)))
                .toList();
        return PageResponse.of(data, conversationsPage.getTotalPages(), conversationsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID currentUserId, UUID conversationId) {
        var conversation = conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, currentUserId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        var memberCount = members.countByConversationIdAndLeftAtIsNull(conversationId);
        var peer = conversation.getType() == ConversationTypeEnum.DIRECT
                ? mapPeer(users.findById(directPeerId(conversation, currentUserId))
                        .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND)))
                : null;
        return conversationMapper.toDetailResponse(conversation, memberCount, peer);
    }

    @Override
    @Transactional
    public ConversationDetailResponse updateConversation(
            UUID currentUserId, UUID conversationId, UpdateConversationRequest request) {
        var conversation = conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict("conversation.directCannotBeUpdated");
        }
        var membership = members.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        if (membership.getRole() != MemberRoleEnum.OWNER
                || !currentUserId.equals(conversation.getOwnerId())) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        validateGroupAvatar(currentUserId, request.avatarFileId());

        conversation.updateGroup(StringUtils.trim(request.name()), request.avatarFileId());
        var memberCount = members.countByConversationIdAndLeftAtIsNull(conversationId);
        return conversationMapper.toDetailResponse(conversation, memberCount, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<ConversationMemberResponse>> getConversationMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size) {
        if (!conversations.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, currentUserId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        var sort = Sort.by(Sort.Order.asc("joinedAt"), Sort.Order.asc("id"));
        var membershipsPage = members.findByConversationIdAndLeftAtIsNull(
                conversationId, PageableUtils.of(page, size, sort));
        if (membershipsPage.isEmpty()) {
            return PageResponse.of(
                    List.of(), membershipsPage.getTotalPages(), membershipsPage.getTotalElements());
        }
        var userIds = membershipsPage.getContent().stream()
                .map(ConversationMemberEntity::getUserId)
                .toList();
        var usersById = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(user -> user.getId(), Function.identity()));
        var data = membershipsPage.getContent().stream()
                .map(membership -> conversationMemberMapper.toResponse(
                        membership,
                        Optional.ofNullable(usersById.get(membership.getUserId()))
                                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND)),
                        presence.status(membership.getUserId())))
                .toList();
        return PageResponse.of(
                data, membershipsPage.getTotalPages(), membershipsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<UserResponse>> getAvailableGroupMembers(
            UUID currentUserId,
            UUID conversationId,
            String keyword,
            Integer page,
            Integer size) {
        var conversation = conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict("conversation.availableMembersGroupOnly");
        }
        var membership = members.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        if (membership.getRole() != MemberRoleEnum.OWNER
                || !currentUserId.equals(conversation.getOwnerId())) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        String resolvedKeyword = StringUtils.trim(keyword);
        if (resolvedKeyword == null) {
            resolvedKeyword = "";
        }
        var sort = Sort.by(Sort.Order.asc("username"), Sort.Order.asc("id"));
        var usersPage = users.searchAvailableGroupMembers(
                currentUserId,
                conversationId,
                resolvedKeyword,
                PageableUtils.of(page, size, sort));
        return PageableUtils.toResponse(
                usersPage, user -> userMapper.toResponse(user, presence.status(user.getId())));
    }

    @Override
    @Transactional
    public AddConversationMembersResponse addConversationMembers(
            UUID currentUserId,
            UUID conversationId,
            AddConversationMembersRequest request) {
        var conversation = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict("conversation.membersGroupOnly");
        }
        var ownerMembership = members.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        if (ownerMembership.getRole() != MemberRoleEnum.OWNER
                || !currentUserId.equals(conversation.getOwnerId())) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        var requestedUserIds = new LinkedHashSet<>(request.memberIds());
        if (users.countByIdIn(requestedUserIds) != requestedUserIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        var membershipsByUserId = members.findByConversationIdAndUserIdIn(
                        conversationId, requestedUserIds).stream()
                .collect(Collectors.toMap(
                        ConversationMemberEntity::getUserId,
                        Function.identity()));
        var addedUserIds = new ArrayList<UUID>();
        var existingUserIds = new ArrayList<UUID>();
        var membershipsToSave = new ArrayList<ConversationMemberEntity>();
        var joinedAt = clock.instant();

        requestedUserIds.forEach(userId -> {
            var membership = membershipsByUserId.get(userId);
            if (membership != null && membership.getLeftAt() == null) {
                existingUserIds.add(userId);
                return;
            }
            if (membership == null) {
                membership = new ConversationMemberEntity(
                        conversationId, userId, MemberRoleEnum.MEMBER, joinedAt);
            } else {
                membership.rejoin(joinedAt);
            }
            membershipsToSave.add(membership);
            addedUserIds.add(userId);
        });
        if (!membershipsToSave.isEmpty()) {
            members.saveAllAndFlush(membershipsToSave);
        }
        return new AddConversationMembersResponse(
                List.copyOf(addedUserIds), List.copyOf(existingUserIds));
    }

    @Override
    @Transactional
    public void removeConversationMember(
            UUID currentUserId,
            UUID conversationId,
            UUID userId) {
        var conversation = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict("conversation.membersGroupOnly");
        }

        var currentMembership = members.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, currentUserId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
        boolean isOwner = currentMembership.getRole() == MemberRoleEnum.OWNER
                && currentUserId.equals(conversation.getOwnerId());
        if (currentUserId.equals(conversation.getOwnerId()) && !isOwner) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        if (currentUserId.equals(userId)) {
            if (isOwner) {
                throw BusinessException.conflict("conversation.ownerCannotLeave");
            }
            currentMembership.leave(clock.instant());
            members.saveAndFlush(currentMembership);
            return;
        }
        if (!isOwner) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        var targetMembership = members.findByConversationIdAndUserId(conversationId, userId);
        if (targetMembership.isEmpty() || targetMembership.get().getLeftAt() != null) {
            return;
        }
        if (targetMembership.get().getRole() != MemberRoleEnum.MEMBER) {
            throw BusinessException.conflict("conversation.ownerCannotLeave");
        }
        targetMembership.get().leave(clock.instant());
        members.saveAndFlush(targetMembership.get());
    }

    private Map<UUID, MessageResponse> mapMessagesWithAttachments(Collection<MessageEntity> messageEntities) {
        if (messageEntities.isEmpty()) {
            return Map.of();
        }
        var messageIds = messageEntities.stream().map(MessageEntity::getId).toList();
        var senderIds = messageEntities.stream().map(MessageEntity::getSenderId).distinct().toList();
        var sendersById = users.findAllById(senderIds).stream().collect(Collectors.toMap(
                user -> user.getId(), Function.identity()));
        var attachmentLinks = attachments.findByMessageIdInOrderByMessageIdAscPositionAsc(messageIds);
        var fileIds = attachmentLinks.stream().map(link -> link.getFileId()).distinct().toList();
        var filesById = fileIds.isEmpty()
                ? Map.<UUID, FileEntity>of()
                : files.findAllById(fileIds).stream().collect(Collectors.toMap(
                        file -> file.getId(), Function.identity()));
        Map<UUID, List<FileEntity>> filesByMessageId = new HashMap<>();
        attachmentLinks.forEach(link -> {
            var file = filesById.get(link.getFileId());
            if (file != null) {
                filesByMessageId.computeIfAbsent(link.getMessageId(), ignored -> new ArrayList<>()).add(file);
            }
        });
        return messageEntities.stream().collect(Collectors.toMap(
                MessageEntity::getId,
                message -> messageMapper.toResponse(
                        message,
                        Optional.ofNullable(sendersById.get(message.getSenderId()))
                                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND)),
                        filesByMessageId.getOrDefault(message.getId(), List.of()))));
    }

    private Map<UUID, UserEntity> loadDirectPeers(
            List<ConversationEntity> conversationEntities, UUID currentUserId) {
        var peerIds = conversationEntities.stream()
                .filter(conversation -> conversation.getType() == ConversationTypeEnum.DIRECT)
                .map(conversation -> directPeerId(conversation, currentUserId))
                .distinct()
                .toList();
        if (peerIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(peerIds).stream()
                .collect(Collectors.toMap(user -> user.getId(), Function.identity()));
    }

    private ConversationPeerResponse peerFor(
            ConversationEntity conversation,
            UUID currentUserId,
            Map<UUID, UserEntity> peersById) {
        if (conversation.getType() != ConversationTypeEnum.DIRECT) {
            return null;
        }
        var peerId = directPeerId(conversation, currentUserId);
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
        var avatar = files.findById(avatarFileId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        boolean allowed = currentUserId.equals(avatar.getUploaderId())
                && avatar.getConversationId() == null
                && StringUtils.isImageContentType(avatar.getContentType());
        if (!allowed) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }
}
