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
public class RoomParticipantPO {
    private Long roomId;

    private Long userId;

    // 参会者角色: 1-普通成员, 2-主持人, 3-联席主持
    private Integer role;

    // 状态: 1-在线, 2-离线(中途退出), 3-被移除
    private Integer status;

    private Boolean audioMuted;

    private Boolean videoMuted;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}