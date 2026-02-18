package com.theoyu.thesis.media.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.media.biz.model.vo.*;
import com.theoyu.thesis.media.biz.service.MediaService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
@Slf4j
public class MediaController {

    @Resource
    private MediaService mediaService;

    /**
     * 启动录制
     */
    @PostMapping("/record/start")
    @ApiOperationLog(description = "启动录制")
    public Response<StartRecordingResVO> startRecording(@RequestBody @Valid StartRecordingReqVO reqVO) {
        log.info("收到启动录制请求: roomId={}, hostId={}", reqVO.getRoomId(), reqVO.getHostId());
        StartRecordingResVO resVO = mediaService.startRecording(reqVO);
        return Response.success(resVO);
    }

    /**
     * 停止录制
     */
    @PostMapping("/record/stop")
    @ApiOperationLog(description = "停止录制")
    public Response<StopRecordingResVO> stopRecording(@RequestBody @Valid StopRecordingReqVO reqVO) {
        log.info("收到停止录制请求: roomId={}, hostId={}", reqVO.getRoomId(), reqVO.getHostId());
        StopRecordingResVO resVO = mediaService.stopRecording(reqVO);
        return Response.success(resVO);
    }

    /**
     * 获取录制状态
     */
    @GetMapping("/record/status")
    @ApiOperationLog(description = "获取录制状态")
    public Response<GetRecordingStatusResVO> getRecordingStatus(
            @RequestParam Long roomId,
            @RequestParam Long hostId) {
        GetRecordingStatusResVO resVO = mediaService.getRecordingStatus(roomId, hostId);
        return Response.success(resVO);
    }
}