package com.theoyu.thesis.media.biz.controller;

import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.service.RoomMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 房间消息 WebSocket 控制器
 */
@Controller
@Slf4j
public class RoomMessageWebSocketController {

    @Resource
    private RoomMessageService roomMessageService;

    /**
     * 接收客户端发送的消息
     * 客户端发送到: /app/room/sendMessage
     */
    @MessageMapping("/room/sendMessage")
    public void handleMessage(@Payload RoomMessageReqVO reqVO,
                              SimpMessageHeaderAccessor headerAccessor) {
        try {
            String userId = (String) headerAccessor.getSessionAttributes().get("userId");

            reqVO.setUserId(Long.valueOf(userId));
            roomMessageService.sendMessage(reqVO);
        } catch (Exception e) {
            log.error("房间内WebSocket消息处理失败", e);
        }
    }
}