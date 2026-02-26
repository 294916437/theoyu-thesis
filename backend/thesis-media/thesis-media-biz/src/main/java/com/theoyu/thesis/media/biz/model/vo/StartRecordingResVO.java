package com.theoyu.thesis.media.biz.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StartRecordingResVO {

    private Long roomId;

    private Long userId;

    /**
     * true = 已存在录制记录，false = 新建
     */
    private Boolean exists;

    /**
     * 已有录制时返回，新建时为 null
     */
    private String fileUrl;

    /**
     * 已有录制时返回（字节）
     */
    private Integer fileSize;

    /**
     * 已有录制时返回（秒）
     */
    private Integer duration;

    private String format;

    private LocalDateTime startTime;

    /**
     * 已有录制时返回
     */
    private LocalDateTime endTime;
}