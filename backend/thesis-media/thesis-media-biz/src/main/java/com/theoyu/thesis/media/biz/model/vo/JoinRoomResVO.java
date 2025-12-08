package com.theoyu.thesis.media.biz.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinRoomResVO {

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * SFU服务器地址
     */
    private String sfuServerUrl;

    /**
     * 是否允许加入
     */
    private Boolean allowed;

    /**
     * 提示信息
     */
    private String message;
}