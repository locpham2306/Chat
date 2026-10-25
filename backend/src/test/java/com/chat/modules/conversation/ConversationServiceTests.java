package com.chat.modules.conversation;

import com.chat.exception.BusinessException;
import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import com.chat.modules.conversation.mapper.ConversationMapper;
import com.chat.modules.conversation.repository.ConversationRepository;
import com.chat.modules.conversation.service.impl.ConversationServiceImpl;
import com.chat.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConversationServiceTests {
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ConversationServiceImpl service = new ConversationServiceImpl(
            conversations, users, new ConversationMapper());

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

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
