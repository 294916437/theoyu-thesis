package com.theoyu.thesis.chat.biz.config;

import com.theoyu.thesis.chat.biz.service.UserOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP 消息拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final UserOnlineService userOnlineService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            handleDisconnect(accessor);
        }
        
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        if (sessionAttributes == null) {
            log.warn("Session attributes 为空");
            return;
        }
        
        Object userIdObj = sessionAttributes.get("userId");
        
        if (userIdObj == null) {
            log.warn("CONNECT 帧中缺少 userId");
            return;
        }
        
        try {
            Long userId = Long.parseLong(userIdObj.toString());
            String sessionId = accessor.getSessionId();
            
            // 设置用户 Principal，让 Spring 能将 userId 映射到 sessionId
            accessor.setUser(new Principal() {
                @Override
                public String getName() {
                    return String.valueOf(userId);  // 返回 userId 作为用户名
                }
                
                @Override
                public String toString() {
                    return "User{id=" + userId + ", sessionId=" + sessionId + "}";
                }
            });
            
            // 设置用户在线状态
            userOnlineService.setUserOnline(userId, sessionId);
        } catch (NumberFormatException e) {
            log.error(String.valueOf(e));
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        if (sessionAttributes == null) {
            return;
        }
        
        Object userIdObj = sessionAttributes.get("userId");
        
        if (userIdObj != null) {
            try {
                Long userId = Long.parseLong(userIdObj.toString());
                String sessionId = accessor.getSessionId();
                
                userOnlineService.setUserOffline(userId, sessionId);
                
            } catch (NumberFormatException e) {
                log.error("userId 格式错误: {}", userIdObj);
            }
        }
    }
}
