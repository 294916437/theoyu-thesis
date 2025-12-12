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
public class ConversationPO {
    private Long id;

    private Integer conversationType;

    private String title;

    private Long lastMessageId;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}