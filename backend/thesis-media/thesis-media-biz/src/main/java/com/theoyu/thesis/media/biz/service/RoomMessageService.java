package com.theoyu.thesis.media.biz.service;

import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;

import java.util.List;

public interface RoomMessageService {
    /**
     * 发送房间消息
     */
    void sendMessage(RoomMessageReqVO reqVO);


    /**
     * 构建消息响应VO(供 gRPC 使用)
     */
    RoomMessageResVO buildMessageResVO(RoomMessagePO messagePO);

    /**
     * 查询房间消息历史
     */
    List<RoomMessageResVO> getMessageHistory(Long roomId, Integer pageNum, Integer pageSize);

}
