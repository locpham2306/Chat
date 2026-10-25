package com.chat.modules.conversation.service.impl;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.NotFoundException;
import com.chat.exception.NoPermissionException;
import com.chat.modules.conversation.dto.request.CreateGroupRequest;
import com.chat.modules.conversation.dto.response.DirectConversationResponse;
import com.chat.modules.conversation.dto.response.GroupConversationResponse;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.ConversationService;
import com.chat.modules.file.repository.FileRepository;
import com.chat.modules.user.repository.UserRepository;
import com.chat.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final FileRepository files;
    private final ConversationMapper conversationMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<DirectConversationResponse> findDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("conversation.recipientIsCurrentUser");
        }
        if (!users.existsById(recipientId)) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
        }
        return conversations.findDirectBetweenUsers(
                        ConversationTypeEnum.DIRECT, currentUserId, recipientId)
                .map(conversationMapper::toDirectResponse);
    }

    @Override
    @Transactional
    public GroupConversationResponse createGroup(UUID currentUserId, CreateGroupRequest request) {
        var memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.remove(currentUserId);
        if (memberIds.isEmpty()) {
            throw new BadRequestException("conversation.groupRequiresMember");
        }
        if (users.countByIdIn(memberIds) != memberIds.size()) {
            throw new NotFoundException(ErrorConstants.NOT_FOUND);
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

    private void validateGroupAvatar(UUID currentUserId, UUID avatarFileId) {
        if (avatarFileId == null) {
            return;
        }
        var avatar = files.findById(avatarFileId)
                .orElseThrow(() -> new NotFoundException(ErrorConstants.NOT_FOUND));
        boolean allowed = currentUserId.equals(avatar.getUploaderId())
                && avatar.getConversationId() == null
                && StringUtils.isImageContentType(avatar.getContentType());
        if (!allowed) {
            throw new NoPermissionException(ErrorConstants.FORBIDDEN);
        }
    }
}
