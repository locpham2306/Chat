package com.chat.modules.websocket.config;

import com.chat.config.CorsProperties;
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

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String IDENTITY_ATTRIBUTE = "com.chat.websocket.sessionIdentity";

    private final CorsProperties cors;

    public WebSocketHandshakeInterceptor(CorsProperties cors) {
        this.cors = cors;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        var identity = servletRequest.getServletRequest()
                .getAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE);
        if (!(identity instanceof SessionIdentity sessionIdentity)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String origin = request.getHeaders().getOrigin();
        if (origin == null || !cors.allowedOrigins().contains(origin)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(IDENTITY_ATTRIBUTE, sessionIdentity);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
