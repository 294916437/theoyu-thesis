package com.theoyu.thesis.media.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;
import com.theoyu.thesis.media.biz.service.RoomMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房间消息 REST API 控制器
 */
@RestController
@RequestMapping("/room/message")  // 统一路径前缀
@Slf4j
public class RoomMessageRestController {

    @Resource
    private RoomMessageService roomMessageService;

    /**
     * 查询房间消息历史
     */
    @GetMapping("/history")
    @ApiOperationLog(description = "查询房间消息历史")
    public Response<List<RoomMessageResVO>> getMessageHistory(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<RoomMessageResVO> result = roomMessageService.getMessageHistory(roomId, page, size);
        return Response.success(result);
    }
}