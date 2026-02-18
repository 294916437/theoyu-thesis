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
public class StopRecordingResVO {

    private Long roomId;

    private Long hostId;

    /**
     * 状态: 0-录制中(RECORDING), 1-上传中(UPLOADING), 2-已完成(COMPLETED), 3-失败(FAILED)
     */
    private Integer status;

    private String fileUrl;

    private Integer fileSize;

    /**
     * 录制时长（秒）
     */
    private Integer duration;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}