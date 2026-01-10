package com.theoyu.thesis.media.biz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从 sessionAttributes 获取握手阶段存入的参数
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes == null) {
                throw new IllegalArgumentException("会话属性缺失");
            }

            String userId = (String) sessionAttributes.get("userId");
            String roomId = (String) sessionAttributes.get("roomId");

            if (userId == null || roomId == null) {
                log.warn("STOMP CONNECT 缺少必要参数: userId={}, roomId={}", userId, roomId);
                throw new IllegalArgumentException("缺少必要参数");
            }
            // 可选: 设置 Principal (用于 /user 订阅)
            accessor.setUser(() -> userId);
        }

        return message;
    }
}