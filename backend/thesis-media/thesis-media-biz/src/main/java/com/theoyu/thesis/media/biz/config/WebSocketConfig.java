package com.theoyu.thesis.media.biz.config;

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
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理,用于广播消息
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目的地前缀
        config.setUserDestinationPrefix("/user");
        log.info("WebSocket消息代理配置完成");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点,允许跨域,支持SockJS
        registry.addEndpoint("/ws/room")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        log.info("WebSocket端点注册完成: /ws/room");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 注册拦截器,用于获取连接参数
        registration.interceptors(webSocketAuthInterceptor);
        log.info("WebSocket拦截器配置完成");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 设置消息大小限制
        registration.setMessageSizeLimit(128 * 1024) // 128KB
                .setSendBufferSizeLimit(512 * 1024) // 512KB
                .setSendTimeLimit(20 * 1000); // 20秒
        log.info("WebSocket传输配置完成");
    }
}