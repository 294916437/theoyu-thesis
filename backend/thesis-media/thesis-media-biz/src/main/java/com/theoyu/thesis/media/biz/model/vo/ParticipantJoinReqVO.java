package com.theoyu.thesis.media.biz.model.vo;

import lombok.Data;

@Data
public class ParticipantJoinReqVO {
    private String roomId;
    private String userId;
    private String username;
    private Long timestamp;
}