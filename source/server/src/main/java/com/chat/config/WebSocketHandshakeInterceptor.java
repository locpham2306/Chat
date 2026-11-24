package com.chat.config;

import com.chat.modules.auth.security.SessionAuthenticationFilter;
import com.chat.modules.auth.service.SessionIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// Kiểm tra session và origin trước khi cho phép mở kết nối WebSocket.
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String IDENTITY_ATTRIBUTE = "com.chat.websocket.sessionIdentity";

    private final CorsProperties corsProperties;

    // Dùng chung danh sách origin đã khai báo trong cấu hình CORS.
    public WebSocketHandshakeInterceptor(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    // Chặn handshake nếu thiếu session hoặc origin không hợp lệ.
    // Hợp lệ thì chuyển danh tính người dùng sang WebSocket session.
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Object identity = servletRequest.getServletRequest()
                .getAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE);
        if (!(identity instanceof SessionIdentity sessionIdentity)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String origin = request.getHeaders().getOrigin();
        if (origin == null || !corsProperties.allowedOrigins().contains(origin)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(IDENTITY_ATTRIBUTE, sessionIdentity);
        return true;
    }

    // Không cần làm gì sau handshake, nhưng interface bắt buộc phải có hàm này.
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Exception exception) {
    }
}
