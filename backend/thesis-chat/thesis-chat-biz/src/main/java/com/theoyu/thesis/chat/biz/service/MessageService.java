package com.theoyu.thesis.chat.biz.service;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.chat.biz.model.vo.GetMessagesReqVO;
import com.theoyu.thesis.chat.biz.model.vo.GetMessagesResVO;
import com.theoyu.thesis.chat.biz.model.vo.SendMessageReqVO;

public interface MessageService {
    /**
     * 发送消息
     */
    Response<?> sendMessage(Long conversationId, SendMessageReqVO reqVO);
    /**
     * 获取消息列表（游标分页）
     */
    Response<GetMessagesResVO> getMessages(Long conversationId, GetMessagesReqVO reqVO);
}
