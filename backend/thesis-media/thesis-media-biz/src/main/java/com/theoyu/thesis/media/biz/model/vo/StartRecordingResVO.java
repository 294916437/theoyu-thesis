package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StartRecordingResVO {

    private Long roomId;

    private Long hostId;

    /**
     * 录制状态：0-录制中 1-上传中 2-已完成 3-失败
     */
    private Integer status;

    private String format;

    private LocalDateTime startTime;

    private String message;
}