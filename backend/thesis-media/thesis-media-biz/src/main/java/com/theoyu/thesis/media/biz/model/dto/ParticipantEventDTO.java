package com.theoyu.thesis.media.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantEventDTO {
    private String roomId;
    private String userId;
    private String username;
    private String eventType; // joined/left/media_changed
    private Long timestamp;
    private Map<String, Object> extraData; // 扩展字段
}