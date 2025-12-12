package com.theoyu.thesis.chat.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationParticipantPO {
    private Long conversationId;

    private Long userId;

    private Boolean isActive;

    private Integer unreadCount;

    private Long lastReadMessageId;

    private LocalDateTime lastReadTime;

    private LocalDateTime joinedTime;

    private LocalDateTime updatedTime;

}