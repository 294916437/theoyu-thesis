package com.theoyu.thesis.chat.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {
    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 图片URL列表
     */
    private List<String> imgUris;

    /**
     * 视频URL
     */
    private String videoUri;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 是否是当前用户发送的
     */
    private Boolean isSelf;
}