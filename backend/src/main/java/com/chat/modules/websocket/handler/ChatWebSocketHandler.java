package com.chat.modules.websocket.handler;

import com.chat.common.constant.ErrorConstants;
import com.chat.exception.BadRequestException;
import com.chat.exception.BusinessException;
import com.chat.exception.UnauthenticatedException;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.message.dto.response.MessageSenderResponse;
import com.chat.modules.message.service.MessageSendCommand;
import com.chat.modules.message.service.MessageService;
import com.chat.modules.websocket.config.WebSocketHandshakeInterceptor;
import com.chat.modules.websocket.dto.MessageAckData;
import com.chat.modules.websocket.dto.NewMessageData;
import com.chat.modules.websocket.dto.SendMessageFrameData;
import com.chat.modules.websocket.dto.TypingData;
import com.chat.modules.websocket.dto.TypingFrameData;
import com.chat.modules.websocket.dto.WebSocketErrorData;
import com.chat.modules.websocket.service.RealtimeConversationService;
import com.chat.modules.websocket.service.WebSocketSessionRegistry;
import com.chat.utils.MessageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    public static final String SEND_MESSAGE = "SEND_MESSAGE";
    public static final String MESSAGE_ACK = "MESSAGE_ACK";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String TYPING = "TYPING";
    public static final String READ_RECEIPT = "READ_RECEIPT";
    public static final String PRESENCE_CHANGED = "PRESENCE_CHANGED";
    public static final String ERROR = "ERROR";

    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final SessionService sessions;
    private final MessageService messages;
    private final RealtimeConversationService realtime;
    private final WebSocketSessionRegistry registry;
    private final MessageUtils messageUtils;

    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            SessionService sessions,
            MessageService messages,
            RealtimeConversationService realtime,
            WebSocketSessionRegistry registry,
            MessageUtils messageUtils) {
        this.objectMapper = objectMapper;
        this.sessions = sessions;
        this.messages = messages;
        this.realtime = realtime;
        this.registry = registry;
        this.messageUtils = messageUtils;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        var identity = identity(session);
        if (identity == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (java.io.IOException exception) {
                LOG.debug("Could not close unauthenticated WebSocket connection {}", session.getId());
            }
            return;
        }
        var registration = registry.register(session, identity);
        if (!sessions.isActive(identity.sessionId())) {
            registry.closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (registration.firstForUser()) {
            registry.runIfOnline(identity.userId(), () -> {
                var change = realtime.connected(identity.userId());
                registry.broadcast(change.audienceUserIds(), PRESENCE_CHANGED, change.data());
            });
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String clientMessageId = null;
        try {
            var identity = requireIdentity(session);
            if (!sessions.isActive(identity.sessionId())) {
                sendError(session, null, ErrorConstants.UNAUTHENTICATED,
                        messageUtils.getMessage(ErrorConstants.UNAUTHENTICATED));
                registry.closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION);
                return;
            }
            JsonNode root = objectMapper.readTree(message.getPayload());
            JsonNode data = root == null ? null : root.get("data");
            JsonNode event = root == null ? null : root.get("event");
            if (data == null || !data.isObject() || event == null || !event.isTextual()) {
                throw invalid();
            }
            clientMessageId = textOrNull(data.get("clientMessageId"));
            switch (event.textValue()) {
                case SEND_MESSAGE -> handleSendMessage(session, identity, data);
                case TYPING -> handleTyping(identity, data);
                default -> throw invalid();
            }
        } catch (BusinessException exception) {
            String code = ErrorConstants.forStatus(exception.getStatus().value());
            String detail = messageUtils.getMessage(exception.getMessageKey(), exception.getArguments());
            if (detail.equals(exception.getMessageKey())) {
                detail = messageUtils.getMessage(code);
            }
            sendError(session, clientMessageId, code, detail);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            sendError(session, clientMessageId, ErrorConstants.VALIDATION_ERROR,
                    messageUtils.getMessage(ErrorConstants.VALIDATION_ERROR));
        } catch (Exception exception) {
            LOG.error("Unhandled WebSocket exception type: {}", exception.getClass().getName());
            sendError(session, clientMessageId, ErrorConstants.INTERNAL_ERROR,
                    messageUtils.getMessage(ErrorConstants.INTERNAL_ERROR));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var disconnection = registry.unregister(session.getId());
        if (disconnection != null && disconnection.lastForUser()) {
            try {
                registry.runIfOffline(disconnection.userId(), () -> {
                    var change = realtime.disconnected(disconnection.userId());
                    registry.broadcast(change.audienceUserIds(), PRESENCE_CHANGED, change.data());
                });
            } catch (BusinessException exception) {
                LOG.debug("Could not update presence for disconnected user {}", disconnection.userId());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        registry.closeConnection(session.getId(), CloseStatus.SERVER_ERROR);
    }

    private void handleSendMessage(
            WebSocketSession session, SessionIdentity identity, JsonNode data) throws JsonProcessingException {
        var request = objectMapper.treeToValue(data, SendMessageFrameData.class);
        var delivery = messages.send(new MessageSendCommand(
                identity.userId(), request.recipientId(), request.conversationId(),
                request.clientMessageId(), request.content(), request.attachmentIds()));
        registry.sendToConnection(session.getId(), MESSAGE_ACK, new MessageAckData(
                delivery.clientMessageId(), delivery.conversationId(), delivery.id(), delivery.createdAt()));
        if (delivery.newlyCreated()) {
            registry.broadcast(delivery.activeMemberIds(), NEW_MESSAGE, new NewMessageData(
                    delivery.id(), delivery.conversationId(), delivery.senderId(),
                    new MessageSenderResponse(
                            delivery.senderId(), delivery.senderFullName(),
                            delivery.senderAvatarFileId()),
                    delivery.clientMessageId(), delivery.content(), delivery.attachments(),
                    delivery.createdAt()));
        }
    }

    private void handleTyping(SessionIdentity identity, JsonNode data) throws JsonProcessingException {
        var request = objectMapper.treeToValue(data, TypingFrameData.class);
        if (request.conversationId() == null || request.isTyping() == null) {
            throw invalid();
        }
        List<java.util.UUID> audience = realtime.typingAudience(
                identity.userId(), request.conversationId());
        registry.broadcast(audience, TYPING, new TypingData(
                request.conversationId(), identity.userId(), request.isTyping()));
    }

    private void sendError(
            WebSocketSession session, String clientMessageId, String code, String detail) {
        registry.sendToConnection(session.getId(), ERROR,
                new WebSocketErrorData(clientMessageId, code, detail));
    }

    private SessionIdentity requireIdentity(WebSocketSession session) {
        var identity = identity(session);
        if (identity == null) {
            throw new UnauthenticatedException(ErrorConstants.UNAUTHENTICATED);
        }
        return identity;
    }

    private SessionIdentity identity(WebSocketSession session) {
        var value = session.getAttributes().get(WebSocketHandshakeInterceptor.IDENTITY_ATTRIBUTE);
        return value instanceof SessionIdentity identity ? identity : null;
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private BadRequestException invalid() {
        return new BadRequestException(ErrorConstants.VALIDATION_ERROR);
    }
}
