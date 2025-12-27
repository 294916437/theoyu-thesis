package com.theoyu.thesis.media.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议创建事件消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreatedEventDTO{
    /**
     * 会议ID
     */
    private Long roomId;

    /**
     * 会议号
     */
    private String roomNo;

    /**
     * 房主ID
     */
    private Long hostId;

    /**
     * 会议标题
     */
    private String title;

    /**
     * 参与者总数
     */
    private Integer totalParticipants;

    /**
     * 房间消息总数
     */
    private Integer totalMessages;

    /**
     * 会议持续时长
     */
    private Integer duration;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
}