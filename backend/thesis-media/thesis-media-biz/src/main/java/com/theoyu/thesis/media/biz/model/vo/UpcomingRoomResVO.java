package com.theoyu.thesis.media.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingRoomResVO {
    /**
     * 会议ID
     */
    private Long roomId;

    /**
     * 会议号
     */
    private String roomNo;

    /**
     * 会议标题
     */
    private String title;

    /**
     * 房主ID
     */
    private Long hostId;

    /**
     * 房主名称
     */
    private String hostName;

    /**
     * 会议类型
     */
    private Integer type;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

    /**
     * 计划开始时间
     */
    private LocalDateTime startTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 是否为房主
     */
    private Boolean isHost;
}