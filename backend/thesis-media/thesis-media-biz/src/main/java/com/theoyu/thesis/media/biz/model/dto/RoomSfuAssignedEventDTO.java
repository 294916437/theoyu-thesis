package com.theoyu.thesis.media.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 房间首次分配 SFU 节点事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSfuAssignedEventDTO {

    private Long roomId;

    private String roomNo;

    private Long sfuNodeId;

    private String instanceId;

    private String sfuServerUrl;

    private Long assignedByUserId;

    private LocalDateTime timestamp;
}
