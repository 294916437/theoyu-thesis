package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartRecordingReqVO {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 录制格式：mp4 / webm
     */
    private String format = "webm";
}