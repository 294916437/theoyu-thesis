package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface RoomParticipantPOMapper {
    int deleteByPrimaryKey(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int insert(RoomParticipantPO record);

    int insertSelective(RoomParticipantPO record);

    RoomParticipantPO selectByPrimaryKey(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int updateByPrimaryKeySelective(RoomParticipantPO record);

    int updateByPrimaryKey(RoomParticipantPO record);

    /**
     * 根据房间ID和用户ID查询参与者
     */
    RoomParticipantPO selectByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 统计房间在线人数
     */
    Long countByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") Integer status);

    /**
     * 更新参与者状态
     */
    int updateStatusByRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("leaveTime") LocalDateTime leaveTime
    );

}