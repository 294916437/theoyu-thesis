package com.theoyu.thesis.chat.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationDetailResVO {
    private Long id;
    private Integer conversationType;
    private String title;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private List<ParticipantVO> participants;
    private LocalDateTime createdTime;
    
    @Data
    public static class ParticipantVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private Boolean isActive;
        private LocalDateTime joinedTime;
    }
}