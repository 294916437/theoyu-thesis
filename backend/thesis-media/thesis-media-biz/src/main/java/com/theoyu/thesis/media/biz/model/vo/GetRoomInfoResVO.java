package com.theoyu.thesis.media.biz.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GetRoomInfoResVO {

    private Long roomId;
    private String roomNo;
    private String title;
    private Long hostId;
    private String hostName;
    private Integer type;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime createdTime;
    private String sfuServerUrl;

    /**
     * 当前在线参与者列表
     */
    private List<ParticipantInfoVO> participants;

    @Data
    @Builder
    public static class ParticipantInfoVO {
        private Long userId;
        private String username;
        private String avatar;
        private Integer role;
        private Boolean audioMuted;
        private Boolean videoMuted;
        private LocalDateTime joinedAt;
    }
}