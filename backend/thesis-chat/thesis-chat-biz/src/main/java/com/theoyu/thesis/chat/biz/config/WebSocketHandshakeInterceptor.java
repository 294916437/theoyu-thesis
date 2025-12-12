package com.theoyu.thesis.chat.biz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 在握手阶段提取 userId 并存储到 session 属性中
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {

            // 从 URL 参数获取 userId
            String userId = servletRequest.getServletRequest().getParameter("userId");
            
            if (userId != null && !userId.isEmpty()) {
                attributes.put("userId", userId);
                return true;
            } else {
                log.warn("WebSocket 握手缺少 userId 参数");
                return false; // 拒绝连接
            }
        }
        
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // 握手后处理（可选）
    }
}