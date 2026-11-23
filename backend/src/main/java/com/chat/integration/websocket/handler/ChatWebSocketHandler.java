package com.chat.integration.websocket.handler;

import com.chat.common.constant.ErrorConstants;
import com.chat.config.WebSocketHandshakeInterceptor;
import com.chat.exception.BusinessException;
import com.chat.integration.websocket.dto.MessageAckData;
import com.chat.integration.websocket.dto.NewMessageData;
import com.chat.integration.websocket.dto.SendMessageFrameData;
import com.chat.integration.websocket.dto.TypingFrameData;
import com.chat.integration.websocket.dto.WebSocketErrorData;
import com.chat.integration.websocket.service.PresenceChange;
import com.chat.integration.websocket.service.RealtimeConversationService;
import com.chat.integration.websocket.service.TypingChange;
import com.chat.integration.websocket.service.WebSocketSessionRegistry;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.auth.service.SessionService;
import com.chat.modules.message.dto.response.MessageSenderResponse;
import com.chat.modules.message.service.MessageDelivery;
import com.chat.modules.message.service.MessageSendCommand;
import com.chat.modules.message.service.MessageService;
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

import java.util.UUID;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    public static final String SEND_MESSAGE = "SEND_MESSAGE";
    public static final String TYPING = "TYPING";
    public static final String MESSAGE_ACK = "MESSAGE_ACK";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String READ_RECEIPT = "READ_RECEIPT";
    public static final String PRESENCE_CHANGED = "PRESENCE_CHANGED";
    public static final String ERROR = "ERROR";

    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final MessageService messageService;
    private final RealtimeConversationService realtimeConversationService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final MessageUtils messageUtils;

    // Nhận các thành phần dùng để đọc JSON, kiểm tra session, xử lý tin và gửi event lên FE.
    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            SessionService sessionService,
            MessageService messageService,
            RealtimeConversationService realtimeConversationService,
            WebSocketSessionRegistry webSocketSessionRegistry,
            MessageUtils messageUtils) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.realtimeConversationService = realtimeConversationService;
        this.webSocketSessionRegistry = webSocketSessionRegistry;
        this.messageUtils = messageUtils;
    }

    // Chạy ngay sau khi handshake thành công.
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Lấy danh tính người đăng nhập
        SessionIdentity identity = identity(session);

        // Handshake bình thường đã gắn identity; nếu thiếu thì đóng connection để tránh truy cập ẩn danh.
        if (identity == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (java.io.IOException exception) {
                LOG.debug("Could not close unauthenticated WebSocket connection {}", session.getId());
            }
            return;
        }

        WebSocketSessionRegistry.Registration registration = webSocketSessionRegistry.register(session, identity);

        // Session có thể vừa hết hạn/đăng xuất giữa lúc handshake và lúc đăng ký
        if (!sessionService.isActive(identity.sessionId())) {
            webSocketSessionRegistry.closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Một user có thể mở nhiều tab; chỉ tab đầu tiên mới làm trạng thái chuyển sang ONLINE
        if (registration.firstForUser()) {
            webSocketSessionRegistry.runIfOnline(
                    identity.userId(), () -> broadcastOnline(identity.userId()));
        }
    }

    // Nhận từng text frame FE gửi xuống BE, kiểm tra event/data rồi chuyển tới hàm xử lý.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Giữ lại clientMessageId để event ERROR chỉ đúng tin nào của FE bị lỗi.
        String clientMessageId = null;
        try {
            SessionIdentity identity = requireIdentity(session);

            // Connection đang mở nhưng session app đã hết hạn/thu hồi thì báo lỗi và đóng ngay.
            if (!sessionService.isActive(identity.sessionId())) {
                sendError(session, null, ErrorConstants.UNAUTHENTICATED,
                        messageUtils.getMessage(ErrorConstants.UNAUTHENTICATED));
                webSocketSessionRegistry.closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION);
                return;
            }

            // Frame hợp lệ phải có dạng { "event": "...", "data": { ... } }.
            JsonNode root = objectMapper.readTree(message.getPayload());
            JsonNode data = root == null ? null : root.get("data");
            JsonNode event = root == null ? null : root.get("event");
            if (data == null || !data.isObject() || event == null || !event.isTextual()) {
                throw invalid();
            }
            clientMessageId = textOrNull(data.get("clientMessageId"));

            // Mỗi event được chuyển sang một hàm riêng; event lạ bị coi là request không hợp lệ.
            switch (event.textValue()) {
                case SEND_MESSAGE -> handleSendMessage(session, identity, data);
                case TYPING -> handleTyping(identity, data);
                default -> throw invalid();
            }
        } catch (BusinessException exception) {
            // Lỗi nghiệp vụ giữ đúng code/message như HTTP rồi được gửi lên FE qua event ERROR.
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

    // Chạy khi một connection bị đóng.
    // Chỉ khi user hết connection mới cập nhật lastSeen và gửi OFFLINE lên FE.
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSessionRegistry.Disconnection disconnection = webSocketSessionRegistry.unregister(session.getId());
        if (disconnection != null && disconnection.lastForUser()) {
            try {
                webSocketSessionRegistry.runIfOffline(
                        disconnection.userId(), () -> broadcastOffline(disconnection.userId()));
            } catch (BusinessException exception) {
                LOG.debug("Could not update presence for disconnected user {}", disconnection.userId());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        webSocketSessionRegistry.closeConnection(session.getId(), CloseStatus.SERVER_ERROR);
    }

    private void handleSendMessage(WebSocketSession session, SessionIdentity identity, JsonNode data) throws JsonProcessingException {
        SendMessageFrameData request = objectMapper.treeToValue(data, SendMessageFrameData.class);

        MessageSendCommand command = new MessageSendCommand(
                identity.userId(),
                request.getRecipientId(),
                request.getConversationId(),
                request.getClientMessageId(),
                request.getContent(),
                request.getAttachmentIds());
        MessageDelivery delivery = messageService.send(command);

        // Gửi ACK lên FE người gửi
        MessageAckData acknowledgment = new MessageAckData(
                delivery.clientMessageId(),
                delivery.conversationId(),
                delivery.id(),
                delivery.createdAt());
        webSocketSessionRegistry.sendToConnection(
                session.getId(), MESSAGE_ACK, acknowledgment);

        // Broadcast NEW_MESSAGE
        if (delivery.newlyCreated()) {
            MessageSenderResponse sender = new MessageSenderResponse(
                    delivery.senderId(),
                    delivery.senderFullName(),
                    delivery.senderAvatarFileId());
            NewMessageData newMessage = new NewMessageData(
                    delivery.id(),
                    delivery.conversationId(),
                    delivery.senderId(),
                    sender,
                    delivery.clientMessageId(),
                    delivery.content(),
                    delivery.attachments(),
                    delivery.createdAt());
            webSocketSessionRegistry.broadcast(
                    delivery.activeMemberIds(), NEW_MESSAGE, newMessage);
        }
    }

    // Kiểm tra TYPING FE gửi xuống BE và tìm các thành viên cần được gửi event lên FE.
    private void handleTyping(SessionIdentity identity, JsonNode data) throws JsonProcessingException {
        TypingFrameData request = objectMapper.treeToValue(data, TypingFrameData.class);
        if (request.getConversationId() == null || request.getTyping() == null) {
            throw invalid();
        }
        TypingChange change = realtimeConversationService.typing(
                identity.userId(), request.getConversationId(), request.getTyping());
        webSocketSessionRegistry.broadcast(change.audienceUserIds(), TYPING, change.data());
    }

    private void sendError(WebSocketSession session, String clientMessageId, String code, String detail) {
        WebSocketErrorData error = new WebSocketErrorData(clientMessageId, code, detail);
        webSocketSessionRegistry.sendToConnection(session.getId(), ERROR, error);
    }

    // User vừa có connection đầu tiên: tạo dữ liệu ONLINE và gửi lên các user liên quan.
    private void broadcastOnline(UUID userId) {
        PresenceChange change = realtimeConversationService.connected(userId);
        webSocketSessionRegistry.broadcast(
                change.audienceUserIds(), PRESENCE_CHANGED, change.data());
    }

    // User vừa mất connection cuối: lưu lastSeen và gửi trạng thái OFFLINE lên FE.
    private void broadcastOffline(UUID userId) {
        PresenceChange change = realtimeConversationService.disconnected(userId);
        webSocketSessionRegistry.broadcast(
                change.audienceUserIds(), PRESENCE_CHANGED, change.data());
    }

    private SessionIdentity requireIdentity(WebSocketSession session) {
        SessionIdentity identity = identity(session);
        if (identity == null) {
            throw BusinessException.unauthenticated(ErrorConstants.UNAUTHENTICATED);
        }
        return identity;
    }

    // Đọc identity đã được WebSocketHandshakeInterceptor gắn vào attributes lúc handshake.
    private SessionIdentity identity(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketHandshakeInterceptor.IDENTITY_ATTRIBUTE);
        return value instanceof SessionIdentity identity ? identity : null;
    }

    // Chỉ lấy text từ JsonNode; node thiếu hoặc sai kiểu thì trả null.
    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private BusinessException invalid() {
        return BusinessException.badRequest(ErrorConstants.VALIDATION_ERROR);
    }
}
