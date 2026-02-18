package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomRecordPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomRecordPOMapper {
    int deleteByPrimaryKey(@Param("roomId") Long roomId, @Param("hostId") Long hostId);

    int insert(RoomRecordPO record);

    int insertSelective(RoomRecordPO record);

    RoomRecordPO selectByPrimaryKey(@Param("roomId") Long roomId, @Param("hostId") Long hostId);

    int updateByPrimaryKeySelective(RoomRecordPO record);

    int updateByPrimaryKey(RoomRecordPO record);

    /**
     * 查询房间内进行中的录制（status=1），一个房间同一时刻只有一条进行中记录
     */
    RoomRecordPO selectActiveByRoomId(@Param("roomId") Long roomId);

    /**
     * 根据 roomId + hostId 更新录制完成信息
     */
    int updateCompletedInfo(RoomRecordPO record);

    /**
     * 查询房间历史录制列表（按创建时间倒序分页）
     */
    List<RoomRecordPO> selectListByRoomId(@Param("roomId") Long roomId);

    /**
     * 查询指定主播在指定房间的录制记录
     */
    RoomRecordPO selectByRoomIdAndHostId(
            @Param("roomId") Long roomId,
            @Param("hostId") Long hostId
    );

}