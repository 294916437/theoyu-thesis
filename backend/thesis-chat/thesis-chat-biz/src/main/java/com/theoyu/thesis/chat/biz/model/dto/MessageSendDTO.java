package com.theoyu.thesis.chat.biz.model.dto;

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
public class MessageSendDTO {
    /**
     * 消息ID
     */
    private Long messageId;
    
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
     * 接收者ID列表
     */
    private List<Long> receiverIds;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 图片 URL 列表
     */
    private List<String> imgUris;
    
    /**
     * 视频 URL
     */
    private String videoUri;
    
    /**
     * 回复的消息 ID
     */
    private Long replyToMessageId;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}