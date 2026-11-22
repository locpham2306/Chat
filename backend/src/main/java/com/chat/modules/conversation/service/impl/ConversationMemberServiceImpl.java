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
import com.chat.modules.user.enums.UserStatusEnum;
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
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final ConversationMemberMapper conversationMemberMapper;
    private final UserMapper userMapper;
    private final UserPresence userPresence;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<List<ConversationMemberResponse>> getMembers(
            UUID currentUserId, UUID conversationId, Integer page, Integer size) {
        requireConversationExists(conversationId);
        requireActiveMemberExists(conversationId, currentUserId);

        Sort sort = Sort.by(Sort.Order.asc("joinedAt"), Sort.Order.asc("id"));
        Page<ConversationMemberEntity> membershipsPage = conversationMemberRepository
                .findByConversationIdAndLeftAtIsNull(
                        conversationId, PageableUtils.of(page, size, sort));
        if (membershipsPage.isEmpty()) {
            return PageResponse.of(
                    List.of(), membershipsPage.getTotalPages(), membershipsPage.getTotalElements());
        }

        List<UUID> userIds = new ArrayList<>(membershipsPage.getNumberOfElements());
        for (ConversationMemberEntity membership : membershipsPage.getContent()) {
            userIds.add(membership.getUserId());
        }
        Map<UUID, UserEntity> usersById = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            usersById.put(user.getId(), user);
        }

        List<ConversationMemberResponse> data = new ArrayList<>();
        for (ConversationMemberEntity membership : membershipsPage.getContent()) {
            UUID userId = membership.getUserId();
            UserEntity user = usersById.get(userId);
            if (user == null) {
                throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
            }
            UserStatusEnum status = userPresence.status(userId);
            ConversationMemberResponse response =
                    conversationMemberMapper.toResponse(membership, user, status);
            data.add(response);
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

        String resolvedKeyword = StringUtils.trim(keyword);
        if (resolvedKeyword == null) {
            resolvedKeyword = "";
        }
        Sort sort = Sort.by(Sort.Order.asc("username"), Sort.Order.asc("id"));
        Page<UserEntity> usersPage = userRepository.searchAvailableGroupMembers(
                currentUserId,
                conversationId,
                resolvedKeyword,
                PageableUtils.of(page, size, sort));

        List<UserResponse> users = new ArrayList<>(usersPage.getNumberOfElements());
        for (UserEntity user : usersPage.getContent()) {
            UserStatusEnum status = userPresence.status(user.getId());
            UserResponse response = userMapper.toResponse(user, status);
            users.add(response);
        }
        return PageResponse.of(users, usersPage.getTotalPages(), usersPage.getTotalElements());
    }

    @Override
    @Transactional
    public AddConversationMembersResponse addMembers(
            UUID currentUserId,
            UUID conversationId,
            AddConversationMembersRequest request) {
        ConversationEntity conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        requireGroup(conversation, "conversation.membersGroupOnly");
        requireOwner(conversation, conversationId, currentUserId);

        LinkedHashSet<UUID> requestedUserIds = new LinkedHashSet<>(request.memberIds());
        if (userRepository.countByIdIn(requestedUserIds) != requestedUserIds.size()) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }

        Map<UUID, ConversationMemberEntity> membershipsByUserId = new HashMap<>();
        List<ConversationMemberEntity> currentMemberships =
                conversationMemberRepository.findByConversationIdAndUserIdIn(
                        conversationId, requestedUserIds);
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
            conversationMemberRepository.saveAllAndFlush(membershipsToSave);
        }
        return new AddConversationMembersResponse(
                List.copyOf(addedUserIds), List.copyOf(existingUserIds));
    }

    @Override
    @Transactional
    public void removeMember(UUID currentUserId, UUID conversationId, UUID userId) {
        ConversationEntity conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
        requireGroup(conversation, "conversation.membersGroupOnly");

        ConversationMemberEntity currentMembership = requireActiveMember(conversationId, currentUserId);
        boolean hasOwnerRole = currentMembership.getRole() == MemberRoleEnum.OWNER;
        boolean isConversationOwner = currentUserId.equals(conversation.getOwnerId());
        boolean isOwner = hasOwnerRole && isConversationOwner;
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
                conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId);
        if (targetMembership.isEmpty()) {
            return;
        }
        ConversationMemberEntity memberToRemove = targetMembership.get();
        if (memberToRemove.getLeftAt() != null) {
            return;
        }
        if (memberToRemove.getRole() != MemberRoleEnum.MEMBER) {
            throw BusinessException.conflict("conversation.ownerCannotLeave");
        }
        leave(memberToRemove);
    }

    private ConversationEntity requireConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }

    private void requireConversationExists(UUID conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
    }

    private ConversationMemberEntity requireActiveMember(UUID conversationId, UUID userId) {
        return conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden(ErrorConstants.FORBIDDEN));
    }

    private void requireActiveMemberExists(UUID conversationId, UUID userId) {
        if (!conversationMemberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, userId)) {
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
        conversationMemberRepository.saveAndFlush(membership);
    }
}
