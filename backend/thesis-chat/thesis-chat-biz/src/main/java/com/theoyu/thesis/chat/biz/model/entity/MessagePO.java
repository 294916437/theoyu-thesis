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
public class MessagePO {
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Integer messageType;

    private String imgUris;

    private String videoUri;

    private String contentUuid;

    private Long replyToMessageId;

    private Boolean isDeleted;

    private LocalDateTime createdTime;

}