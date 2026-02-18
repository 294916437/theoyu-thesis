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
public class RecordingStartedEventDTO {

    /**
     * 联合主键：房间ID
     */
    private Long roomId;

    /**
     * 联合主键：主持人ID
     */
    private Long hostId;

    /**
     * 录制格式，如 webm/mp4
     */
    private String format;

    private LocalDateTime startTime;

    private LocalDateTime timestamp;
}