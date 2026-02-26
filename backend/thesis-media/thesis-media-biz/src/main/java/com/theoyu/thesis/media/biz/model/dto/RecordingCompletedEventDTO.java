package com.theoyu.thesis.media.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordingCompletedEventDTO {

    /**
     * 联合主键：房间ID
     */
    private Long roomId;

    /**
     * 联合主键：主持人ID
     */
    private Long hostId;

    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Integer fileSize;

    /**
     * 录制时长（秒）
     */
    private Integer duration;

    private LocalDateTime endTime;
}