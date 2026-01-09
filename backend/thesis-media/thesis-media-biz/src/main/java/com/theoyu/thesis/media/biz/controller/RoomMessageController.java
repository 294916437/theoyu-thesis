package com.theoyu.thesis.media.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;
import com.theoyu.thesis.media.biz.service.RoomMessageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Slf4j
public class RoomMessageController {

    @Resource
    private RoomMessageService roomMessageService;

    /**
     * 接收客户端发送的消息
     * 客户端发送到: /app/room/sendMessage
     */
    @MessageMapping("/room/sendMessage")
    public void handleMessage(@Payload RoomMessageReqVO reqVO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 从会话中获取用户信息(可选,用于额外验证)
            String userId = (String) headerAccessor.getSessionAttributes().get("userId");
            String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

            log.info("收到WebSocket消息, userId: {}, roomId: {}, ", userId, roomId);

            // 调用业务逻辑处理消息(会自动广播)
            reqVO.setUserId(Long.valueOf(userId));
            roomMessageService.sendMessage(reqVO);
        } catch (Exception e) {
            log.error("WebSocket消息处理失败", e);
        }
    }

    /**
     * 查询房间消息历史 (HTTP)
     */
    @GetMapping("/room/message/history")
    @ApiOperationLog(description = "查询房间消息历史")
    public Response<List<RoomMessageResVO>> getMessageHistory(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<RoomMessageResVO> result = roomMessageService.getMessageHistory(roomId, page, size);
        return Response.success(result);
    }
}