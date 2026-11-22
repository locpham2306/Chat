package com.chat.integration.websocket.service;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BusinessException;
import com.chat.integration.websocket.dto.PresenceChangedData;
import com.chat.integration.websocket.dto.RealtimeUserData;
import com.chat.integration.websocket.dto.TypingData;
import com.chat.modules.conversation.repository.ConversationMemberRepository;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.user.entity.UserEntity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// File này xử lý nghiệp vụ trước khi BE gửi typing và presence lên FE.
@Service
@RequiredArgsConstructor
public class RealtimeConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // Xử lý event đang gõ: kiểm tra hội thoại và quyền thành viên rồi lấy những
    // người còn lại làm audience.
    @Transactional(readOnly = true)
    public TypingChange typing(UUID userId, UUID conversationId, boolean isTyping) {
        if (conversationId == null || !conversationRepository.existsById(conversationId)) {
            throw BusinessException.notFound(ErrorConstants.NOT_FOUND);
        }
        if (!conversationMemberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, userId)) {
            throw BusinessException.forbidden(ErrorConstants.FORBIDDEN);
        }
        UserEntity user = findUser(userId);

        // Không gửi TYPING lên FE của chính người đang gõ.
        List<UUID> activeUserIds =
                conversationMemberRepository.findActiveUserIds(conversationId);
        List<UUID> audience = new ArrayList<>();
        for (UUID activeUserId : activeUserIds) {
            if (!activeUserId.equals(userId)) {
                audience.add(activeUserId);
            }
        }

        RealtimeUserData realtimeUser = toRealtimeUser(user);
        TypingData data = new TypingData(conversationId, userId, realtimeUser, isTyping);
        return new TypingChange(data, audience);
    }

    // Tạo ONLINE và tìm những user mà BE cần gửi PRESENCE_CHANGED lên FE.
    @Transactional(readOnly = true)
    public PresenceChange connected(UUID userId) {
        UserEntity user = findUser(userId);
        RealtimeUserData realtimeUser = toRealtimeUser(user);
        PresenceChangedData data = new PresenceChangedData(
                userId, realtimeUser, UserStatusEnum.ONLINE, user.getLastSeen());
        List<UUID> audience =
                conversationMemberRepository.findActiveSharedConversationUserIds(userId);
        return new PresenceChange(data, audience);
    }

    // Khi user mất connection cuối, lưu lastSeen rồi tạo OFFLINE để BE gửi lên FE.
    @Transactional
    public PresenceChange disconnected(UUID userId) {
        UserEntity user = findUser(userId);
        Instant lastSeen = clock.instant();
        user.markLastSeen(lastSeen);

        RealtimeUserData realtimeUser = toRealtimeUser(user);
        PresenceChangedData data = new PresenceChangedData(
                userId, realtimeUser, UserStatusEnum.OFFLINE, lastSeen);
        List<UUID> audience =
                conversationMemberRepository.findActiveSharedConversationUserIds(userId);
        return new PresenceChange(data, audience);
    }

    // Rút gọn UserEntity thành thông tin công khai BE được phép gửi lên FE.
    private RealtimeUserData toRealtimeUser(UserEntity user) {
        return new RealtimeUserData(
                user.getId(), user.getFullName(), user.getAvatarFileId());
    }

    // Tìm user hoặc trả lỗi 404 nếu dữ liệu session/user không còn hợp lệ.
    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound(ErrorConstants.NOT_FOUND));
    }
}
