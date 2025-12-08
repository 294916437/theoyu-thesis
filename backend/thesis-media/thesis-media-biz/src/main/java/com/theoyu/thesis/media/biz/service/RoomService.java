package com.theoyu.thesis.media.biz.service;

import com.theoyu.thesis.media.biz.model.vo.*;

public interface RoomService {

    /**
     * 创建房间
     */
    CreateRoomResVO createRoom(CreateRoomReqVO reqVO);

    /**
     * 获取房间信息
     */
    GetRoomInfoResVO getRoomInfo(Long roomId);

    /**
     * 加入房间（预验证）
     */
    JoinRoomResVO joinRoom(JoinRoomReqVO reqVO);

    /**
     * 关闭房间
     */
    void closeRoom(Long roomId);
}
