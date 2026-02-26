package com.theoyu.thesis.media.biz.service;


import com.theoyu.thesis.media.biz.model.vo.*;

public interface MediaService {

    /**
     * 开始录制
     * 幂等接口：roomId + userId 已存在录制记录时直接返回已有信息
     */
    StartRecordingResVO startRecording(StartRecordingReqVO reqVO);

    /**
     * 停止录制
     * 必须在前端 uploadFile 上传完成拿到 fileUrl 后调用
     */
    StopRecordingResVO stopRecording(StopRecordingReqVO reqVO);



}
