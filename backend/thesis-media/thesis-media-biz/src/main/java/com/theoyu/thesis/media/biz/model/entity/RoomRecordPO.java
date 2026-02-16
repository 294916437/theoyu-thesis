package com.theoyu.thesis.media.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomRecordPO {
    private Long roomId;

    private Long hostId;

    private String fileUrl;

    private Integer fileSize;

    private Integer duration;

    private String format;

    private Byte status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}