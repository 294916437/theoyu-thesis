package com.theoyu.thesis.media.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;
import com.theoyu.thesis.media.biz.service.RoomMessageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room/message")
@Slf4j
public class RoomMessageController {

    @Resource
    private RoomMessageService roomMessageService;

    /**
     * 发送房间消息 (WebSocket)
     */
    @MessageMapping("/send")
    public void sendMessage(@Payload @Valid RoomMessageReqVO reqVO,
                            SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到房间消息发送请求, roomId: {}", reqVO.getRoomId());

        try {
            // 从会话属性中获取用户信息
            Object userId = headerAccessor.getSessionAttributes().get("userId");
            Object roomId = headerAccessor.getSessionAttributes().get("roomId");

            log.info("WebSocket会话信息, userId: {}, roomId: {}", userId, roomId);

            // 发送消息
            RoomMessageResVO resVO = roomMessageService.sendMessage(reqVO);
            log.info("房间消息发送成功, messageId: {}", resVO.getMessageId());
        } catch (Exception e) {
            log.error("房间消息发送失败", e);
        }
    }

    /**
     * 查询房间消息历史 (HTTP)
     */
    @GetMapping("/history")
    @ResponseBody
    public Response<List<RoomMessageResVO>> getMessageHistory(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("查询房间消息历史, roomId: {}, pageNum: {}, pageSize: {}", roomId, pageNum, pageSize);
        List<RoomMessageResVO> result = roomMessageService.getMessageHistory(roomId, pageNum, pageSize);
        return Response.success(result);
    }

}