package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartRecordingReqVO {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "主持人ID不能为空")
    private Long hostId;

    /**
     * 录制格式，默认 webm
     */
    private String format = "webm";

    /**
     * 是否录制音频，默认 true
     */
    private Boolean audioEnabled = true;

    /**
     * 是否录制视频，默认 true
     */
    private Boolean videoEnabled = true;

    /**
     * 视频质量（low/medium/high），默认 medium
     */
    private String quality = "medium";
}