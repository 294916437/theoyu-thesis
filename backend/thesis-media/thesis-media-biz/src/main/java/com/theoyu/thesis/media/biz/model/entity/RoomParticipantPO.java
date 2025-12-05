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

    private Integer role;

    private Integer status;

    private Boolean audioMuted;

    private Boolean videoMuted;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}