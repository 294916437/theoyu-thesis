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
public class ConversationListResVO {
    private List<ConversationItemVO> conversations;
    private Long nextCursor; // 下一页游标
    private Boolean hasMore; // 是否还有更多数据
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationItemVO {
        private Long id;
        private Integer conversationType;
        private String title;
        private Long lastMessageId;
        private String lastMessageContent;
        private LocalDateTime lastMessageTime;
        private Integer unreadCount;
        private Boolean isActive;
        
        // 对方用户信息（私聊时）
        private UserVO user;
    }
    
    /**
     * 用户信息VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserVO {
        private Long userId;
        private String nickname;
        private String avatar;
    }
}