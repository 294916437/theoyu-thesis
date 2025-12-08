package com.theoyu.thesis.media.biz.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateRoomResVO {

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 房间号（用于用户输入加入）
     */
    private String roomNo;

    /**
     * 房间标题
     */
    private String title;

    /**
     * SFU服务器地址
     */
    private String sfuServerUrl;

    /**
     * 最大参与者数量
     */
    private Integer maxParticipants;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}