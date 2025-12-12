package com.theoyu.thesis.chat.biz.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * 配置 WebSocket 信令服务
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Resource
    private WebSocketHandshakeInterceptor handshakeInterceptor;
    @Resource
    private  StompChannelInterceptor stompChannelInterceptor;


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理 (支持信令和聊天)
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用目标前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目标前缀
        config.setUserDestinationPrefix("/user");

    }

    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 满足WebRTC的信令端点配置
        registry.addEndpoint("/ws/signal")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor);
        // 配置聊天场景的WebSocket端点
            registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(handshakeInterceptor);

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 注册 STOMP 消息拦截器
        registration.interceptors(stompChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(512 * 1024)
                .setSendBufferSizeLimit(1024 * 1024)
                .setSendTimeLimit(20 * 1000);

    }
    
}