package com.theoyu.thesis.media.biz.controller;

import com.theoyu.framework.common.response.PageResponse;
import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.media.biz.model.vo.*;
import com.theoyu.thesis.media.biz.service.RoomService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media/room")
@Slf4j
public class RoomController {

    @Resource
    private RoomService roomService;

    /**
     * 创建会议
     * POST /media/room/create
     */
    @PostMapping("/create")
    @ApiOperationLog(description = "创建会议")
    public Response<CreateRoomResVO> createRoom(@RequestBody @Valid CreateRoomReqVO reqVO) {
        CreateRoomResVO resVO = roomService.createRoom(reqVO);
        return Response.success(resVO);
    }

    /**
     * 获取会议信息
     * GET /media/room/{roomId}
     */
    @GetMapping("/{roomId}")
    @ApiOperationLog(description = "获取会议信息")
    public Response<GetRoomInfoResVO> getRoomInfo(@PathVariable Long roomId) {
        GetRoomInfoResVO resVO = roomService.getRoomInfo(roomId);
        return Response.success(resVO);
    }

    /**
     * 加入会议（预验证）
     * POST /media/room/join
     */
    @PostMapping("/join")
    @ApiOperationLog(description = "加入会议")
    public Response<JoinRoomResVO> joinRoom(@RequestBody @Valid JoinRoomReqVO reqVO) {
        JoinRoomResVO resVO = roomService.joinRoom(reqVO);
        return Response.success(resVO);
    }

    /**
     * 获取即将开始的会议
     * GET /media/room/recent?page=1&size=10
     */
    @GetMapping("/recent")
    @ApiOperationLog(description = "获取最近参加过的会议")
    public PageResponse<RecentRoomResVO> getRecentRooms(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return roomService.getRecentRooms(page, size);
    }

    /**
     * 获取即将开始的会议
     * GET /media/room/upcoming?page=1&size=10
     */
    @GetMapping("/upcoming")
    @ApiOperationLog(description = "获取即将开始的会议")
    public PageResponse<UpcomingRoomResVO> getUpcomingRooms(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "5") Long size) {
        return roomService.getUpcomingRooms(page, size);
    }

    /**
     * 关闭会议
     * POST /media/room/close
     */
    @PostMapping("/close")
    @ApiOperationLog(description = "关闭会议")
    public Response<?> closeRoom(@RequestBody @Valid CloseRoomReqVO reqVO) {
        roomService.closeRoom(reqVO.getRoomId());
        return Response.success();
    }
}