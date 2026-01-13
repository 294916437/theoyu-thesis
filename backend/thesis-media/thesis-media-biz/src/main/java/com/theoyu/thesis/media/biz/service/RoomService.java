package com.theoyu.thesis.media.biz.service;

import com.theoyu.framework.common.response.PageResponse;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.vo.*;

public interface RoomService {

    /**
     * 创建房间
     */
    CreateRoomResVO createRoom(CreateRoomReqVO reqVO);

    /**
     * 获取房间信息
     */
    GetRoomInfoResVO getRoomInfo(String roomIdOrNo);

    /**
     * 获取房间信息
     */
    GetRoomDetailResVO getRoomDetail(String roomIdOrNo);

    /**
     * 加入房间（预验证）
     */
    JoinRoomResVO joinRoom(JoinRoomReqVO reqVO);

    /**
     * 关闭房间
     */
    void closeRoom(Long roomId);

    /**
     * 获取最近参加过的会议
     */
    PageResponse<RecentRoomResVO> getRecentRooms(Long page, Long size);

    /**
     * 获取即将开始的会议
     */
    PageResponse<UpcomingRoomResVO> getUpcomingRooms(Long page, Long size);
    /**
     * 获取房间参与者列表
     */
    PageResponse<ParticipantListItemVO> getParticipants(GetParticipantsReqVO reqVO);

    void removeOnlineParticipantFromCache(Long roomId, Long participantId);

    void addOnlineParticipantToCache(Long roomId, RoomParticipantPO participant, Long userId);


}
