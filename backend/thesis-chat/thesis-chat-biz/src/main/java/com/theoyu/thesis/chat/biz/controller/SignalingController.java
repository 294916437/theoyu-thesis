package com.theoyu.thesis.chat.biz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.chat.biz.model.entity.SignalMessage;
import com.theoyu.thesis.chat.biz.service.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final SignalingService signalingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @MessageMapping("/call-offer")
    @ApiOperationLog(description = "发起通话")
    public void handleCallOffer(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        try {
            // 手动构建对象
            SignalMessage.CallOffer offer = objectMapper.convertValue(payload, SignalMessage.CallOffer.class);
            
            // 从 Session 获取真实用户ID
            Long fromUserId = getUserIdFromSession(accessor);
            offer.setFromUserId(fromUserId);
            
            signalingService.handleCallOffer(offer);
            
            // 转发给目标用户
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(offer.getToUserId()),
                    "/queue/call-offer",
                    offer
            );
        } catch (Exception e) {
            log.error("处理通话邀请失败", e);
            Long fromUserId = getUserIdFromSession(accessor);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(fromUserId),
                    "/queue/error",
                    Map.of("error", "处理通话邀请失败: " + e.getMessage())
            );
        }
    }

    @MessageMapping("/call-answer")
    @ApiOperationLog(description = "接听通话")
    public void handleCallAnswer(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        try {
            SignalMessage.CallAnswer answer = objectMapper.convertValue(payload, SignalMessage.CallAnswer.class);
            
            Long fromUserId = getUserIdFromSession(accessor);
            answer.setFromUserId(fromUserId);
            
            log.info("收到通话应答 - from: {}, to: {}, callId: {}",
                    answer.getFromUserId(), answer.getToUserId(), answer.getCallId());

            signalingService.handleCallAnswer(answer);
            
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(answer.getToUserId()),
                    "/queue/call-answer",
                    answer
            );
            
            log.info("通话应答已转发给用户: {}", answer.getToUserId());
        } catch (Exception e) {
            log.error("处理通话应答失败", e);
        }
    }

    @MessageMapping("/ice-candidate")
    @ApiOperationLog(description = "处理 ICE 候选")
    public void handleIceCandidate(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        try {
            SignalMessage.IceCandidate candidate = objectMapper.convertValue(payload, SignalMessage.IceCandidate.class);
            
            Long fromUserId = getUserIdFromSession(accessor);
            candidate.setFromUserId(fromUserId);
            
            signalingService.handleIceCandidate(candidate);
            
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(candidate.getToUserId()),
                    "/queue/ice-candidate",
                    candidate
            );
        } catch (Exception e) {
            log.error("处理 ICE 候选失败", e);
        }
    }

    @MessageMapping("/call-end")
    @ApiOperationLog(description = "通话结束")
    public void handleCallEnd(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        try {
            SignalMessage.CallEnd callEnd = objectMapper.convertValue(payload, SignalMessage.CallEnd.class);
            
            Long fromUserId = getUserIdFromSession(accessor);
            callEnd.setFromUserId(fromUserId);

            signalingService.handleCallEnd(callEnd);
            
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(callEnd.getToUserId()),
                    "/queue/call-end",
                    callEnd
            );
            
            log.info("通话结束消息已转发给用户: {}", callEnd.getToUserId());
        } catch (Exception e) {
            log.error("处理通话结束失败", e);
        }
    }
    
    /**
     * 从 Session 中提取用户ID
     */
    private Long getUserIdFromSession(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        if (sessionAttributes == null) {
            throw new IllegalStateException("会话属性为空");
        }
        
        Object userIdObj = sessionAttributes.get("userId");
        
        if (userIdObj == null) {
            throw new IllegalStateException("无法获取用户ID");
        }
        
        return Long.parseLong(userIdObj.toString());
    }
}