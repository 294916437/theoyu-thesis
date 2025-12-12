package com.theoyu.thesis.chat.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMessagesResVO {
    /**
     * 消息列表
     */
    private List<MessageVO> messages;

    /**
     * 下一页游标（最后一条消息的ID）
     */
    private Long nextCursor;

    /**
     * 是否还有更多消息
     */
    private Boolean hasMore;
}