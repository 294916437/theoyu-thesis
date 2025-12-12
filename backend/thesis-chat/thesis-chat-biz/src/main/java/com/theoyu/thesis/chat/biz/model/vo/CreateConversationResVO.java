package com.theoyu.thesis.chat.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateConversationResVO {
    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 会话类型：1-单聊
     */
    private Integer conversationType;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 对方用户ID
     */
    private Long targetUserId;
    /**
     * 是否为新创建（true-新创建，false-已存在）
     */
    private Boolean isNew;
}