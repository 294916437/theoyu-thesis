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

    // 会议类型: 1-即时会议, 2-预约会议
    private Integer type;

    private Integer maxParticipants;

    // 房间状态: 0-预约中, 1-进行中, 2-已结束, 3-已取消
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private String settings;

}