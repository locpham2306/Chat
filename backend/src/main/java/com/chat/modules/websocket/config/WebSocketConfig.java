package com.chat.modules.websocket.config;

import com.chat.config.CorsProperties;
import com.chat.modules.websocket.handler.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final CorsProperties cors;

    public WebSocketConfig(
            ChatWebSocketHandler handler,
            WebSocketHandshakeInterceptor handshakeInterceptor,
            CorsProperties cors) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.cors = cors;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(cors.allowedOrigins().toArray(String[]::new));
    }

    @Bean
    public static ServletServerContainerFactoryBean webSocketContainer() {
        var container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(8 * 1024);
        container.setMaxSessionIdleTimeout(90_000L);
        return container;
    }

    @Bean(name = "webSocketTaskScheduler")
    public static ThreadPoolTaskScheduler webSocketTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("chat-ws-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
