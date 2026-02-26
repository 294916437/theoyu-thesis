package com.theoyu.thesis.media.biz.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StopRecordingResVO {

    private String fileUrl;

    private Integer fileSize;

    private Integer duration;

    private LocalDateTime endTime;
}