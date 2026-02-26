package com.theoyu.thesis.media.biz.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StopRecordingReqVO {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "文件URL不能为空")
    private String fileUrl;

    @NotNull(message = "文件大小不能为空")
    private Integer fileSize;

    @NotNull(message = "录制时长不能为空")
    private Integer duration;

    private String format;
}