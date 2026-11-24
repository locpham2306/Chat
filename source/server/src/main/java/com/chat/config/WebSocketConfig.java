package com.chat.config;

import com.chat.integration.websocket.handler.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

// Cấu hình endpoint và tài nguyên dùng cho WebSocket.
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    private final CorsProperties corsProperties;

    // Nhận handler, bộ kiểm tra handshake và danh sách origin được phép kết nối.
    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            WebSocketHandshakeInterceptor webSocketHandshakeInterceptor,
            CorsProperties corsProperties) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.webSocketHandshakeInterceptor = webSocketHandshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    // Mở endpoint /ws và gắn bước kiểm tra session/origin trước khi kết nối.
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
    }

    // Giới hạn kích thước frame và thời gian kết nối được phép không hoạt động.
    @Bean
    public static ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(8 * 1024);
        container.setMaxSessionIdleTimeout(90_000L);
        return container;
    }

    // Tạo luồng chạy heartbeat và các tác vụ định kỳ của WebSocket.
    @Bean(name = "webSocketTaskScheduler")
    public static ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("chat-ws-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
