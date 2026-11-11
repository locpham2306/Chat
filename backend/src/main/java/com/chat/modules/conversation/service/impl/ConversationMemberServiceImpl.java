package com.chat.modules.conversation.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.dto.response.PageResponse;
import com.chat.exception.BusinessException;
import com.chat.modules.conversation.dto.request.AddConversationMembersRequest;
import com.chat.modules.conversation.dto.response.AddConversationMembersResponse;
import com.chat.modules.conversation.dto.response.ConversationMemberResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMemberMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.ConversationMemberService;
import com.chat.modules.user.dto.response.UserResponse;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.mapper.UserMapper;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.user.service.UserPresence;
import com.chat.utils.PageableUtils;
import com.chat.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
@RequiredArgsConstructor
public class ConversationMemberServiceImpl implements ConversationMemberService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final ConversationMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final UserPresence presence;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<ConversationMemberResponse>> getMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size) {
        requireConversationExists(conversationId);
        requireActiveMemberExists(conversationId, currentUserId);

        Sort sort = Sort.by(Sort.Order.asc("joinedAt"), Sort.Order.asc("id"));
        Page<ConversationMemberEntity> membershipsPage = members.findByConversationIdAndLeftAtIsNull(
                conversationId, PageableUtils.of(page, size, sort));
        if (membershipsPage.isEmpty()) {
            return PageResponse.of(
                    List.of(), membershipsPage.getTotalPages(), membershipsPage.getTotalElements());
        }

        List<UUID> userIds = membershipsPage.getContent().stream()
                .map(ConversationMemberEntity::getUserId)
                .toList();
        Map<UUID, UserEntity> usersById = new HashMap<>();
        for (UserEntity user : users.findAllById(userIds)) {
            usersById.put(user.getId(), user);
        }

        List<ConversationMemberResponse> data = new ArrayList<>();
        for (ConversationMemberEntity membership : membershipsPage.getContent()) {
            UUID userId = membership.getUserId();
            UserEntity user = usersById.get(userId);
            if (user == null) {
                throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
            }
            data.add(memberMapper.toResponse(membership, user, presence.status(userId)));
        }
        return PageResponse.of(
                data, membershipsPage.getTotalPages(), membershipsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<UserResponse>> getAvailableMembers(
            UUID currentUserId,
            UUID conversationId,
            String keyword,
            Integer page,
            Integer size) {
        ConversationEntity conversation = requireConversation(conversationId);
        requireGroup(conversation, "conversation.availableMembersGroupOnly");
        requireOwner(conversation, conversationId, currentUserId);

        String resolvedKeyword = Optional.ofNullable(StringUtils.trim(keyword)).orElse("");
        Sort sort = Sort.by(Sort.Order.asc("username"), Sort.Order.asc("id"));
        Page<UserEntity> usersPage = users.searchAvailableGroupMembers(
                currentUserId,
                conversationId,
                resolvedKeyword,
                PageableUtils.of(page, size, sort));
        return PageableUtils.toResponse(
                usersPage, user -> userMapper.toResponse(user, presence.status(user.getId())));
    }

    @Override
    @Transactional
    public AddConversationMembersResponse addMembers(
            UUID currentUserId,
            UUID conversationId,
            AddConversationMembersRequest request) {
        ConversationEntity conversation = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        requireGroup(conversation, "conversation.membersGroupOnly");
        requireOwner(conversation, conversationId, currentUserId);

        LinkedHashSet<UUID> requestedUserIds = new LinkedHashSet<>(request.memberIds());
        if (users.countByIdIn(requestedUserIds) != requestedUserIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }

        Map<UUID, ConversationMemberEntity> membershipsByUserId = new HashMap<>();
        List<ConversationMemberEntity> currentMemberships =
                members.findByConversationIdAndUserIdIn(conversationId, requestedUserIds);
        for (ConversationMemberEntity membership : currentMemberships) {
            membershipsByUserId.put(membership.getUserId(), membership);
        }
        ArrayList<UUID> addedUserIds = new ArrayList<>();
        ArrayList<UUID> existingUserIds = new ArrayList<>();
        ArrayList<ConversationMemberEntity> membershipsToSave = new ArrayList<>();
        Instant joinedAt = clock.instant();

        for (UUID userId : requestedUserIds) {
            ConversationMemberEntity membership = membershipsByUserId.get(userId);
            if (membership != null && membership.getLeftAt() == null) {
                existingUserIds.add(userId);
                continue;
            }
            if (membership == null) {
                membership = new ConversationMemberEntity(
                        conversationId, userId, MemberRoleEnum.MEMBER, joinedAt);
            } else {
                membership.rejoin(joinedAt);
            }
            membershipsToSave.add(membership);
            addedUserIds.add(userId);
        }

        if (!membershipsToSave.isEmpty()) {
            members.saveAllAndFlush(membershipsToSave);
        }
        return new AddConversationMembersResponse(
                List.copyOf(addedUserIds), List.copyOf(existingUserIds));
    }

    @Override
    @Transactional
    public void removeMember(UUID currentUserId, UUID conversationId, UUID userId) {
        ConversationEntity conversation = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        requireGroup(conversation, "conversation.membersGroupOnly");

        ConversationMemberEntity currentMembership = requireActiveMember(conversationId, currentUserId);
        boolean isOwner = currentMembership.getRole() == MemberRoleEnum.OWNER
                && currentUserId.equals(conversation.getOwnerId());
        if (currentUserId.equals(conversation.getOwnerId()) && !isOwner) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        if (currentUserId.equals(userId)) {
            if (isOwner) {
                throw BusinessException.conflict("conversation.ownerCannotLeave");
            }
            leave(currentMembership);
            return;
        }
        if (!isOwner) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }

        Optional<ConversationMemberEntity> targetMembership =
                members.findByConversationIdAndUserId(conversationId, userId)
                .filter(membership -> membership.getLeftAt() == null);
        if (targetMembership.isEmpty()) {
            return;
        }
        if (targetMembership.get().getRole() != MemberRoleEnum.MEMBER) {
            throw BusinessException.conflict("conversation.ownerCannotLeave");
        }
        leave(targetMembership.get());
    }

    private ConversationEntity requireConversation(UUID conversationId) {
        return conversations.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }

    private void requireConversationExists(UUID conversationId) {
        if (!conversations.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
    }

    private ConversationMemberEntity requireActiveMember(UUID conversationId, UUID userId) {
        return members.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
    }

    private void requireActiveMemberExists(UUID conversationId, UUID userId) {
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }

    private void requireGroup(ConversationEntity conversation, String messageKey) {
        if (conversation.getType() != ConversationTypeEnum.GROUP) {
            throw BusinessException.conflict(messageKey);
        }
    }

    private void requireOwner(ConversationEntity conversation, UUID conversationId, UUID userId) {
        ConversationMemberEntity membership = requireActiveMember(conversationId, userId);
        if (membership.getRole() != MemberRoleEnum.OWNER
                || !userId.equals(conversation.getOwnerId())) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
    }

    private void leave(ConversationMemberEntity membership) {
        membership.leave(clock.instant());
        members.saveAndFlush(membership);
    }
}
