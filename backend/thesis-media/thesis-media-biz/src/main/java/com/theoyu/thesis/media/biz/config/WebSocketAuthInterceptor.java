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

@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从原生头信息中获取查询参数
            List<String> userIdList = accessor.getNativeHeader("userId");
            List<String> roomIdList = accessor.getNativeHeader("roomId");

            if (userIdList != null && !userIdList.isEmpty() && roomIdList != null && !roomIdList.isEmpty()) {
                String userId = userIdList.get(0);
                String roomId = roomIdList.get(0);

                // 将用户信息存储到会话属性中
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("roomId", roomId);

                log.info("WebSocket连接建立, userId: {}, roomId: {}", userId, roomId);
            } else {
                log.warn("WebSocket连接缺少必要参数");
            }
        }

        return message;
    }
}