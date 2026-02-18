package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StopRecordingReqVO {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "主持人ID不能为空")
    private Long hostId;
}