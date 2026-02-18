package com.theoyu.thesis.media.biz.service;


import com.theoyu.thesis.media.biz.model.vo.*;

public interface MediaService {

    /**
     * 启动录制
     */
    StartRecordingResVO startRecording(StartRecordingReqVO reqVO);

    /**
     * 停止录制
     */
    StopRecordingResVO stopRecording(StopRecordingReqVO reqVO);

    /**
     * 获取录制状态
     */
    GetRecordingStatusResVO getRecordingStatus(Long roomId, Long hostId);



}
