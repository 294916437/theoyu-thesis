package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomParticipantPOMapper {
    int deleteByPrimaryKey(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int insert(RoomParticipantPO record);

    void insertOrUpdate(RoomParticipantPO participant);


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
    Integer countByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") Integer status);

    /**
     * 查询房间所有在线参与者
     */
    List<RoomParticipantPO> selectByRoomIdAndStatus(
            @Param("roomId") Long roomId,
            @Param("status") Integer status
    );

    List<RoomParticipantPO> selectRecentRoomsByUserId(@Param("userId") Long userId,@Param("offset") Long offset,@Param("size") Long size);

    /**
     * 更新参与者离线状态
     */
    int updateStatusToOffline(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("leftAt") LocalDateTime leftAt
    );

    /**
     * 更新参与者状态为在线
     */
    int updateStatusToOnline(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("updatedTime") LocalDateTime updatedTime
    );

    /**
     * 根据 roomId 和 userId 更新参会者信息
     */
    int updateByRoomIdAndUserId(RoomParticipantPO record);

    /**
     * 批量更新房间所有在线参与者状态为已离开
     */
    int batchUpdateStatusByRoomId(
            @Param("roomId") Long roomId,
            @Param("newStatus") Integer newStatus,
            @Param("oldStatus") Integer oldStatus,
            @Param("leaveTime") LocalDateTime leaveTime
    );
    /**
     * 分页查询房间参与者(按状态过滤)
     */
    List<RoomParticipantPO> selectByRoomIdAndStatusWithPage(
            @Param("roomId") Long roomId,
            @Param("status") Integer status,
            @Param("offset") long offset,
            @Param("size") long size
    );

    /**
     * 分页查询房间所有参与者
     */
    List<RoomParticipantPO> selectByRoomIdWithPage(
            @Param("roomId") Long roomId,
            @Param("offset") long offset,
            @Param("size") long size
    );






}