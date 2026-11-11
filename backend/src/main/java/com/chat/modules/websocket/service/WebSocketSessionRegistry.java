package com.chat.modules.websocket.service;

import com.chat.modules.auth.service.SessionIdentity;
import com.chat.modules.user.enums.UserStatusEnum;
import com.chat.modules.user.service.UserPresence;
import com.chat.modules.websocket.dto.WebSocketFrame;
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

@Component
public class WebSocketSessionRegistry implements UserPresence {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionRegistry.class);
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 256 * 1024;

    private final ObjectMapper objectMapper;
    private final TaskScheduler scheduler;
    private final Map<String, Connection> connections = new HashMap<>();
    private final Map<UUID, Set<String>> connectionIdsByUser = new HashMap<>();
    private final Map<UUID, Set<String>> connectionIdsByAppSession = new HashMap<>();

    public WebSocketSessionRegistry(ObjectMapper objectMapper, TaskScheduler webSocketTaskScheduler) {
        this.objectMapper = objectMapper;
        this.scheduler = webSocketTaskScheduler;
    }

    public synchronized Registration register(WebSocketSession session, SessionIdentity identity) {
        var decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
        boolean firstForUser = !connectionIdsByUser.containsKey(identity.userId());
        var connection = new Connection(decorated, identity.userId(), identity.sessionId());
        connections.put(session.getId(), connection);
        connectionIdsByUser.computeIfAbsent(identity.userId(), ignored -> new LinkedHashSet<>())
                .add(session.getId());
        connectionIdsByAppSession.computeIfAbsent(identity.sessionId(), ignored -> new LinkedHashSet<>())
                .add(session.getId());
        connection.expiryTask = scheduler.schedule(
                () -> closeConnection(session.getId(), CloseStatus.POLICY_VIOLATION), identity.expiresAt());
        return new Registration(firstForUser);
    }

    public synchronized Disconnection unregister(String connectionId) {
        var connection = connections.remove(connectionId);
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

    public void sendToConnection(String connectionId, String event, Object data) {
        var connection = connection(connectionId);
        if (connection != null) {
            send(connection, event, data);
        }
    }

    public void broadcast(Collection<UUID> userIds, String event, Object data) {
        for (var connection : connectionsFor(userIds)) {
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
        var connection = connection(connectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.session.close(status);
        } catch (IOException exception) {
            LOG.debug("Could not close WebSocket connection {}", connectionId);
        }
    }

    @Override
    public synchronized UserStatusEnum status(UUID userId) {
        return connectionIdsByUser.containsKey(userId)
                ? UserStatusEnum.ONLINE : UserStatusEnum.OFFLINE;
    }

    public synchronized void runIfOnline(UUID userId, Runnable action) {
        if (connectionIdsByUser.containsKey(userId)) {
            action.run();
        }
    }

    public synchronized void runIfOffline(UUID userId, Runnable action) {
        if (!connectionIdsByUser.containsKey(userId)) {
            action.run();
        }
    }

    @Scheduled(fixedDelayString = "${app.websocket.heartbeat-interval:30000}")
    public void heartbeat() {
        List<Connection> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(connections.values());
        }
        snapshot.forEach(this::ping);
    }

    private void ping(Connection connection) {
        try {
            connection.session.sendMessage(new PingMessage());
        } catch (IOException | IllegalStateException exception) {
            closeConnection(connection.session.getId(), CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    private void send(Connection connection, String event, Object data) {
        try {
            var payload = objectMapper.writeValueAsString(new WebSocketFrame<>(event, data));
            connection.session.sendMessage(new TextMessage(payload));
        } catch (JsonProcessingException exception) {
            LOG.error("Could not serialize WebSocket event {}", event);
        } catch (IOException | IllegalStateException exception) {
            closeConnection(connection.session.getId(), CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    private synchronized Connection connection(String connectionId) {
        return connections.get(connectionId);
    }

    private synchronized List<Connection> connectionsFor(Collection<UUID> userIds) {
        var ids = new LinkedHashSet<String>();
        for (var userId : userIds) {
            ids.addAll(connectionIdsByUser.getOrDefault(userId, Set.of()));
        }
        var result = new ArrayList<Connection>(ids.size());
        ids.forEach(id -> {
            var connection = connections.get(id);
            if (connection != null) {
                result.add(connection);
            }
        });
        return result;
    }

    private static void remove(Map<UUID, Set<String>> index, UUID key, String connectionId) {
        var ids = index.get(key);
        if (ids == null) {
            return;
        }
        ids.remove(connectionId);
        if (ids.isEmpty()) {
            index.remove(key);
        }
    }

    public record Registration(boolean firstForUser) {
    }

    public record Disconnection(UUID userId, boolean lastForUser) {
    }

    private static final class Connection {
        private final WebSocketSession session;
        private final UUID userId;
        private final UUID appSessionId;
        private ScheduledFuture<?> expiryTask;

        private Connection(WebSocketSession session, UUID userId, UUID appSessionId) {
            this.session = session;
            this.userId = userId;
            this.appSessionId = appSessionId;
        }
    }
}
