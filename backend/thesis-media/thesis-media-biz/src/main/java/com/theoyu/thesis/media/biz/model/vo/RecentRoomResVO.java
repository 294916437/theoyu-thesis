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
public class RecentRoomResVO {
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
     * 会议状态
     */
    private Integer status;

    /**
     * 参与时间
     */
    private LocalDateTime joinedAt;

    /**
     * 离开时间
     */
    private LocalDateTime leftAt;

    /**
     * 会议开始时间
     */
    private LocalDateTime startTime;

    /**
     * 会议结束时间
     */
    private LocalDateTime endTime;

    /**
     * 参与角色（1-房主 2-参与者）
     */
    private Integer role;
}