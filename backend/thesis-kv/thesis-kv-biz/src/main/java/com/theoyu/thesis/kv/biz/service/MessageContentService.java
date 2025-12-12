package com.theoyu.thesis.kv.biz.service;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.dto.request.AddMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindMessageContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindMessageContentRspDTO;

/**
 * 消息内容 Service
 */
public interface MessageContentService {

    /**
     * 添加消息内容
     * @param reqDTO 请求参数
     */
    Response<?> addMessageContent(AddMessageContentReqDTO reqDTO);

    /**
     * 查询消息内容
     * @param reqDTO 请求参数
     * @return 消息内容
     */
    Response<FindMessageContentRspDTO> findMessageContent(FindMessageContentReqDTO reqDTO);

    /**
     * 删除消息内容
     * @param reqDTO 请求参数
     */
    Response<?> deleteMessageContent(DeleteMessageContentReqDTO reqDTO);
}