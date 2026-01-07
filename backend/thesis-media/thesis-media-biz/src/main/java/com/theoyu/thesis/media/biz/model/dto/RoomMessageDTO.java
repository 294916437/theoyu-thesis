package com.theoyu.thesis.media.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMessageDTO implements Serializable {
    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型: 1-文本 2-图片 3-文件
     */
    private Integer messageType;

    /**
     * 发送时间戳
     */
    private Long sendTime;
}