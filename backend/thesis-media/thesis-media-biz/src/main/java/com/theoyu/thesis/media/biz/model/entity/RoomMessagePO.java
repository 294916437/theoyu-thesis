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
public class RoomMessagePO {
    private Long id;

    private Long roomId;

    private Long senderId;

    // 消息类型(1-系统消息 2-用户消息)
    private Integer messageType;
    // 消息内容类型(1-文本 2-图片 3-文件)
    private Integer contentType;

    private String content;

    private Boolean isRecalled;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}