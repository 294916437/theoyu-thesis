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
@RequestMapping("/media/recording")
@Slf4j
public class MediaController {

    @Resource
    private MediaService mediaService;

    /**
     * 开始录制（幂等：已有记录直接返回）
     */
    @PostMapping("/start")
    @ApiOperationLog(description = "开始录制")
    public Response<StartRecordingResVO> startRecording(@RequestBody @Valid StartRecordingReqVO reqVO) {
        log.info("[Recording] 开始录制请求: roomId={}, userId={}", reqVO.getRoomId(), reqVO.getUserId());
        return Response.success(mediaService.startRecording(reqVO));
    }

    /**
     * 停止录制（前端上传文件后调用）
     */
    @PostMapping("/stop")
    @ApiOperationLog(description = "停止录制")
    public Response<StopRecordingResVO> stopRecording(@RequestBody @Valid StopRecordingReqVO reqVO) {
        log.info("[Recording] 停止录制请求: roomId={}, userId={}", reqVO.getRoomId(), reqVO.getUserId());
        return Response.success(mediaService.stopRecording(reqVO));
    }
}