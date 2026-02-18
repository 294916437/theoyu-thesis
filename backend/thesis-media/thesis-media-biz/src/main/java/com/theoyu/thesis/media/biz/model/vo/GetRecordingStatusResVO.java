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
public class GetRecordingStatusResVO {

    private Long roomId;

    private Long hostId;

    /**
     * 是否正在录制
     */
    private Boolean isRecording;

    /**
     * 录制状态：0-录制中 1-上传中 2-已完成 3-失败
     */
    private Integer status;

    /**
     * 当前录制时长（秒）
     */
    private Integer durationSeconds;

    /**
     * 当前文件大小（字节）
     */
    private Long fileSizeBytes;

    private LocalDateTime startTime;

    private String format;
}