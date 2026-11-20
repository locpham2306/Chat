package com.chat.integration.websocket.service;

import com.chat.integration.websocket.dto.WebSocketFrame;
import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.service.UserPresence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

// File này quản lý toàn bộ connection WebSocket đang mở của backend.
// Bộ nhớ runtime quản lý toàn bộ connection WebSocket đang mở trên instance backend này.
// Registry thực sự gửi event lên FE, tính ONLINE/OFFLINE và đóng connection khi session hết hạn.
@Component
public class WebSocketSessionRegistry implements UserPresence {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    // Ngăn một lần gửi chiếm luồng quá lâu hoặc buffer của connection tăng không
    // giới hạn.
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 256 * 1024;

    private final ObjectMapper objectMapper;
    private final TaskScheduler webSocketTaskScheduler;

    // connectionId -> connection WebSocket thật cùng userId và session đăng nhập.
    // Ví dụ WS_1 -> Connection(tab laptop, USER_NAM, SESSION_LAPTOP).
    // Dùng để gửi dữ liệu hoặc đóng chính xác một connection khi đã biết connectionId.
    private final Map<String, Connection> connections = new HashMap<>();

    // userId -> tất cả connectionId của user đó.
    // Ví dụ Nam mở hai tab laptop và một app điện thoại:
    // USER_NAM -> [WS_1, WS_2, WS_3].
    // Dùng để gửi event lên mọi tab/thiết bị và kiểm tra user còn ONLINE hay không.
    private final Map<UUID, Set<String>> connectionIdsByUser = new HashMap<>();

    // sessionId trong bảng sessions -> các connectionId dùng chung phiên đăng nhập đó.
    // Ví dụ SESSION_LAPTOP -> [WS_1, WS_2], SESSION_PHONE -> [WS_3].
    // Khi logout laptop, backend chỉ đóng WS_1 và WS_2; WS_3 trên điện thoại vẫn hoạt động.
    private final Map<UUID, Set<String>> connectionIdsByAppSession = new HashMap<>();

    // Dùng ObjectMapper để tạo JSON và scheduler để hẹn đóng connection đúng lúc
    // session hết hạn.
    public WebSocketSessionRegistry(ObjectMapper objectMapper, TaskScheduler webSocketTaskScheduler) {
        this.objectMapper = objectMapper;
        this.webSocketTaskScheduler = webSocketTaskScheduler;
    }

    // Đăng ký một connection mới vào cả ba map.
    // synchronized để hai connection mở cùng lúc không làm hỏng các map không
    // thread-safe.
    public synchronized Registration register(WebSocketSession session, SessionIdentity identity) {
        // Decorator đảm bảo nhiều luồng không ghi đồng thời trực tiếp lên cùng
        // WebSocketSession.
        ConcurrentWebSocketSessionDecorator decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);

        // Dùng kết quả này để chỉ gửi ONLINE lên FE khi user mở connection đầu tiên.
        boolean firstForUser = !connectionIdsByUser.containsKey(identity.userId());
        Connection connection = new Connection(decorated, identity.userId(), identity.sessionId());
        connections.put(session.getId(), connection);
        connectionIdsByUser.computeIfAbsent(identity.userId(), ignored -> new LinkedHashSet<>())
                .add(session.getId());
        connectionIdsByAppSession.computeIfAbsent(identity.sessionId(), ignored -> new LinkedHashSet<>())
                .add(session.getId());

        connection.expiryTask = webSocketTaskScheduler.schedule(
                () -> closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION), identity.expiresAt());
        return new Registration(firstForUser);
    }

    public synchronized Disconnection unregister(String connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection == null) {
            return null;
        }
        if (connection.expiryTask != null) {
            connection.expiryTask.cancel(false);
        }
        remove(connectionIdsByAppSession, connection.appSessionId, connectionId);
        remove(connectionIdsByUser, connection.userId, connectionId);
        return new Disconnection(connection.userId, !connectionIdsByUser.containsKey(connection.userId));
    }

    // dùng cho MESSAGE_ACK và ERROR.
    public void sendToConnection(String connectionId, String event, Object data) {
        Connection connection = connection(connectionId);
        if (connection != null) {
            send(connection, event, data);
        }
    }

    public void broadcast(Collection<UUID> userIds, String event, Object data) {
        for (Connection connection : connectionsFor(userIds)) {
            send(connection, event, data);
        }
    }

    public void closeAppSession(UUID appSessionId) {
        List<String> ids;
        synchronized (this) {
            ids = List.copyOf(connectionIdsByAppSession.getOrDefault(appSessionId, Set.of()));
        }
        ids.forEach(id -> closeConnection(id, CloseStatus.POLICY_VIOLATION));
    }

    public void closeConnection(String connectionId, CloseStatus status) {
        Connection connection = connection(connectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.session.close(status);
        } catch (IOException exception) {
            LOG.debug("Could not close WebSocket connection {}", connectionId);
        }
    }

    // User được coi là ONLINE nếu registry còn ít nhất một connection của user đó.
    @Override
    public synchronized UserStatusEnum status(UUID userId) {
        return connectionIdsByUser.containsKey(userId)
                ? UserStatusEnum.ONLINE
                : UserStatusEnum.OFFLINE;
    }

    public synchronized void runIfOnline(UUID userId, Runnable action) {
        if (connectionIdsByUser.containsKey(userId)) {
            action.run();
        }
    }

    // Chỉ chạy action nếu user hết connection trước khi gửi OFFLINE lên FE.
    public synchronized void runIfOffline(UUID userId, Runnable action) {
        if (!connectionIdsByUser.containsKey(userId)) {
            action.run();
        }
    }

    // Định kỳ gửi Ping lên FE để phát hiện connection đã chết.
    @Scheduled(fixedDelayString = "${app.websocket.heartbeat-interval:30000}")
    public void heartbeat() {
        List<Connection> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(connections.values());
        }
        snapshot.forEach(this::ping);
    }

    // Gửi một Ping frame lên FE; gửi thất bại thì đóng connection.
    private void ping(Connection connection) {
        try {
            connection.session.sendMessage(new PingMessage());
        } catch (IOException | IllegalStateException exception) {
            closeConnection(connection.session.getId(), CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    // Bọc event/data thành WebSocketFrame, đổi sang JSON rồi gửi lên FE qua
    // connection.
    private void send(Connection connection, String event, Object data) {
        try {
            String payload = objectMapper.writeValueAsString(new WebSocketFrame<>(event, data));
            connection.session.sendMessage(new TextMessage(payload));
        } catch (JsonProcessingException exception) {
            LOG.error("Could not serialize WebSocket event {}", event);
        } catch (IOException | IllegalStateException exception) {
            closeConnection(connection.session.getId(), CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    // Lấy connection theo id trong vùng synchronized để tránh đọc map đồng thời lúc
    // đang sửa.
    private synchronized Connection connection(String connectionId) {
        return connections.get(connectionId);
    }

    // Từ danh sách user, gom mọi connection tương ứng và loại connectionId bị
    // trùng.
    private synchronized List<Connection> connectionsFor(Collection<UUID> userIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (UUID userId : userIds) {
            ids.addAll(connectionIdsByUser.getOrDefault(userId, Set.of()));
        }
        ArrayList<Connection> result = new ArrayList<>(ids.size());
        ids.forEach(id -> {
            Connection connection = connections.get(id);
            if (connection != null) {
                result.add(connection);
            }
        });
        return result;
    }

    // Xóa connectionId khỏi một index; user/session không còn connection thì xóa
    // luôn key rỗng.
    private static void remove(Map<UUID, Set<String>> index, UUID key, String connectionId) {
        Set<String> ids = index.get(key);
        if (ids == null) {
            return;
        }
        ids.remove(connectionId);
        if (ids.isEmpty()) {
            index.remove(key);
        }
    }

    // firstForUser cho biết connection vừa đăng ký có làm user chuyển OFFLINE ->
    // ONLINE hay không.
    public record Registration(boolean firstForUser) {
    }

    // lastForUser cho biết connection vừa đóng có làm user chuyển ONLINE -> OFFLINE
    // hay không.
    public record Disconnection(UUID userId, boolean lastForUser) {
    }

    // Dữ liệu nội bộ gắn một WebSocket connection với user, session đăng nhập và
    // lịch hết hạn.
    private static final class Connection {
        private final WebSocketSession session;
        private final UUID userId;
        private final UUID appSessionId;
        private ScheduledFuture<?> expiryTask;

        // Mỗi connection luôn thuộc đúng một user và một session đăng nhập.
        private Connection(WebSocketSession session, UUID userId, UUID appSessionId) {
            this.session = session;
            this.userId = userId;
            this.appSessionId = appSessionId;
        }
    }
}
