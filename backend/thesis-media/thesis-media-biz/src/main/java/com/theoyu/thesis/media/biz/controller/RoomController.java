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
@RequestMapping("/room")
@Slf4j
public class RoomController {

    @Resource
    private RoomService roomService;


    /**
     * 创建会议
     * POST /room/create
     */
    @PostMapping("/create")
    @ApiOperationLog(description = "创建会议")
    public Response<CreateRoomResVO> createRoom(@RequestBody @Valid CreateRoomReqVO reqVO) {
        CreateRoomResVO resVO = roomService.createRoom(reqVO);
        return Response.success(resVO);
    }

    /**
     * 获取会议信息
     * GET /room/info/{roomIdOrNo}
     */
    @GetMapping("/info/{roomIdOrNo}")
    @ApiOperationLog(description = "获取会议信息")
    public Response<GetRoomInfoResVO> getRoomInfo(@PathVariable String roomIdOrNo) {
        GetRoomInfoResVO resVO = roomService.getRoomInfo(roomIdOrNo);
        return Response.success(resVO);
    }

    /**
     * 获取会议详情
     * GET /room/detail/{roomIdOrNo}
     */
    @GetMapping("/detail/{roomIdOrNo}")
    @ApiOperationLog(description = "获取会议详情")
    public Response<GetRoomDetailResVO> getRoomDetail(@PathVariable String roomIdOrNo) {
        GetRoomDetailResVO resVO = roomService.getRoomDetail(roomIdOrNo);
        return Response.success(resVO);
    }

    /**
     * 加入会议（预验证）
     * POST /room/join
     */
    @PostMapping("/join")
    @ApiOperationLog(description = "加入会议")
    public Response<JoinRoomResVO> joinRoom(@RequestBody @Valid JoinRoomReqVO reqVO) {
        JoinRoomResVO resVO = roomService.joinRoom(reqVO);
        return Response.success(resVO);
    }

    /**
     * 获取即将开始的会议
     * GET /room/recent?page=1&size=10
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
     * GET /room/upcoming?page=1&size=10
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
     * POST /room/close
     */
    @PostMapping("/close")
    @ApiOperationLog(description = "关闭会议")
    public Response<?> closeRoom(@RequestBody @Valid CloseRoomReqVO reqVO) {
        roomService.closeRoom(reqVO.getRoomId());
        return Response.success();
    }

    /**
     * 更新会议信息
     * PUT /room/{roomId}
     */
    @PutMapping("/{roomId}")
    @ApiOperationLog(description = "更新会议信息")
    public Response<?> updateRoom(@PathVariable Long roomId, @RequestBody @Valid UpdateRoomReqVO reqVO) {
        roomService.updateRoom(roomId, reqVO);
        return Response.success();
    }

    /**
     * 删除会议
     * DELETE /room/{roomId}
     */
    @DeleteMapping("/{roomId}")
    @ApiOperationLog(description = "删除会议")
    public Response<?> deleteRoom(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId);
        return Response.success();
    }
    /**
     * 获取房间参与者列表
     * GET /room/participants?roomId=123&status=1&page=1&size=20
     */
    @GetMapping("/participants")
    @ApiOperationLog(description = "获取房间参与者列表")
    public PageResponse<ParticipantListItemVO> getParticipants(@Valid GetParticipantsReqVO reqVO) {
        return roomService.getParticipants(reqVO);
    }

}