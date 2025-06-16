package com.ptit.google.veo3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration cho real-time notifications
 * 
 * Cấu hình này cho phép:
 * - Client kết nối WebSocket qua endpoint /ws
 * - Sử dụng STOMP protocol để pub/sub messages
 * - Message broker cho phép gửi notification đến specific users
 * 
 * Flow hoạt động:
 * 1. Client connect đến /ws endpoint
 * 2. Client subscribe vào /user/{username}/notifications
 * 3. Server send notification đến specific user thông qua SimpMessagingTemplate
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Cấu hình STOMP endpoint để client có thể kết nối
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho WebSocket connection
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins - trong production nên restrict
                .withSockJS(); // Fallback support cho browsers không support WebSocket
        
        log.info("WebSocket STOMP endpoint registered at /ws with SockJS fallback");
    }

    /**
     * Cấu hình message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker cho destination prefixes
        config.enableSimpleBroker("/topic", "/user");
        
        // Prefix cho app destination mapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        config.setUserDestinationPrefix("/user");
        
        log.info("Message broker configured with prefixes: /topic, /user, /app");
    }
}