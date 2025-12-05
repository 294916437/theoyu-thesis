package com.theoyu.thesis.media.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomMessagePO {
    private Long id;

    private Long roomId;

    private Long senderId;

    private Integer messageType;

    private Integer contentType;

    private String contentUuid;

    private Boolean isRecalled;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}