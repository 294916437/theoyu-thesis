package com.theoyu.thesis.chat.biz.controller;

import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.chat.biz.model.vo.SendMessageReqVO;
import com.theoyu.thesis.chat.biz.service.MessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Slf4j
public class MessageChatController {
    
    @Resource
    private MessageService messageService;
    

    /**
     * 接收客户端发送的消息
     * 客户端发送: /app/chat/{conversationId}/send
     */
    @MessageMapping("/chat/{conversationId}/send")
    @ApiOperationLog(description = "WebSocket发送消息")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Validated @Payload SendMessageReqVO reqVO,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        Long senderId = extractUserId(principal, headerAccessor);
        log.info("==> WebSocket接收消息, conversationId: {}, senderId: {}", conversationId, senderId);

        // 设置到 ThreadLocal，让业务层可以获取
        LoginUserContextHolder.setUserId(senderId);

        // 调用业务逻辑
        messageService.sendMessage(conversationId, reqVO);

    }
    /**
     * 提取用户ID（多种方式兼容）
     */
    private Long extractUserId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        // 方式1：从 Principal 获取（优先）
        if (principal != null) {
            try {
                String principalName = principal.getName();
                log.info("Principal.getName(): {}", principalName);
                return Long.parseLong(principalName);
            } catch (NumberFormatException e) {
                log.warn("Principal.getName() 不是有效的 userId: {}", principal.getName());
            }
        }

        // 方式2：从 Session Attributes 获取（备选）
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object userIdObj = headerAccessor.getSessionAttributes().get("userId");

            if (userIdObj != null) {
                try {
                    log.info("Session userId: {}", userIdObj);
                    return Long.parseLong(userIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("Session userId 格式错误: {}", userIdObj);
                }
            }
        }

        return null;
    }
}