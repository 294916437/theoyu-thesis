package com.theoyu.thesis.media.biz.service;

import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;

import java.util.List;

public interface RoomMessageService {
    /**
     * 发送房间消息
     */
    RoomMessageResVO sendMessage(RoomMessageReqVO reqVO);

    /**
     * 查询房间消息历史
     */
    List<RoomMessageResVO> getMessageHistory(Long roomId, Integer pageNum, Integer pageSize);

}
