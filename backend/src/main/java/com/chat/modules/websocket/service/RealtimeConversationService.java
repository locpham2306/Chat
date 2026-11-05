package com.chat.modules.websocket.service;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.repository.UserRepository;
import com.chat.modules.websocket.dto.PresenceChangedData;
import com.chat.modules.websocket.dto.TypingData;
import com.chat.modules.websocket.dto.TypingUserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealtimeConversationService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TypingChange typing(UUID userId, UUID conversationId, boolean isTyping) {
        if (conversationId == null || !conversations.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        var user = findUser(userId);
        var audience = members.findActiveUserIds(conversationId).stream()
                .filter(candidate -> !candidate.equals(userId))
                .toList();
        return new TypingChange(
                new TypingData(
                        conversationId,
                        userId,
                        new TypingUserData(
                                user.getId(), user.getFullName(), user.getAvatarFileId()),
                        isTyping),
                audience);
    }

    @Transactional(readOnly = true)
    public PresenceChange connected(UUID userId) {
        var user = findUser(userId);
        return new PresenceChange(
                new PresenceChangedData(userId, UserStatusEnum.ONLINE, user.getLastSeen()),
                members.findActiveSharedConversationUserIds(userId));
    }

    @Transactional
    public PresenceChange disconnected(UUID userId) {
        var user = findUser(userId);
        var lastSeen = clock.instant();
        user.markLastSeen(lastSeen);
        return new PresenceChange(
                new PresenceChangedData(userId, UserStatusEnum.OFFLINE, lastSeen),
                members.findActiveSharedConversationUserIds(userId));
    }

    private UserEntity findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }
}
