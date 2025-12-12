package com.theoyu.thesis.chat.biz.service;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.chat.biz.model.vo.*;

public interface ConversationService {
    /**
     * 获取会话列表
     * @param reqVO 请求参数（包含userId和cursor）
     * @return 会话列表响应
     */
    Response<ConversationListResVO> getConversationList(ConversationListReqVO reqVO);

    /**
     * 获取会话详情
     * @param conversationId 会话ID
     * @return 会话详情响应
     */
    Response<ConversationDetailResVO> getConversationDetail(Long conversationId);
    /**
     * 创建会话
     * @param reqVO
     * @return 会话详情响应
     */
    Response<CreateConversationResVO> createConversation(CreateConversationReqVO reqVO);
    /**
     * 退出/删除会话
     */
    Response<?> leaveConversation(Long conversationId);

}