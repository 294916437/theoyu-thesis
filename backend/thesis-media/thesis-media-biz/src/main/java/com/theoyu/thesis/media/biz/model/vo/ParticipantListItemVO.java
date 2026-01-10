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
public class ParticipantListItemVO {
    private Long userId;
    private String userName;
    private String avatar;
    private Integer role;
    private Integer status;
    private Boolean audioMuted;
    private Boolean videoMuted;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}