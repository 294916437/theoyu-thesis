package com.theoyu.thesis.media.biz.config;

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
 * 在握手阶段提取 userId 和 roomId 并存储到 session 属性中
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes){

        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从 URL 参数获取 userId 和 roomId
            String userId = servletRequest.getServletRequest().getParameter("userId");
            String roomId = servletRequest.getServletRequest().getParameter("roomId");

            // 验证必要参数
            if (userId != null && !userId.isEmpty() && roomId != null && !roomId.isEmpty()) {
                // 存入 session attributes (后续 STOMP 拦截器可以访问)
                attributes.put("userId", userId);
                attributes.put("roomId", roomId);

                log.info("WebSocket 握手成功, userId: {}, roomId: {}", userId, roomId);
                return true;
            } else {
                log.warn("WebSocket 握手失败: 缺少必要参数 (userId={}, roomId={})", userId, roomId);
                return false; // 拒绝连接
            }
        }

        log.warn("非 ServletServerHttpRequest 类型,跳过参数提取");
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.error("WebSocket 握手后发生异常", exception);
        } else {
            log.debug("WebSocket 握手完成");
        }
    }
}