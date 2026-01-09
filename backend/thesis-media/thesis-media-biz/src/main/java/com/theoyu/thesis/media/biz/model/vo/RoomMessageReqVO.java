package com.theoyu.thesis.media.biz.model.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RoomMessageReqVO {
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 房间ID
     */
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 消息类型: 1-文本 2-图片 3-文件
     */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    /**
     * 消息类型: 1-文本 2-图片 3-文件
     */
    @NotNull(message = "消息内容类型不能为空")
    private Integer contentType;
}