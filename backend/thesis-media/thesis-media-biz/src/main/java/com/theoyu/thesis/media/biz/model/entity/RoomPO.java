package com.theoyu.thesis.media.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomPO {
    private Long id;

    private String roomNo;

    private Long hostId;

    private Long sfuNodeId;

    private String title;

    private Integer type;

    private Integer maxParticipants;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private String settings;


}