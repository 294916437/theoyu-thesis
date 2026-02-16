package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomRecordPO;
import org.apache.ibatis.annotations.Param;

public interface RoomRecordPOMapper {
    int deleteByPrimaryKey(@Param("roomId") Long roomId, @Param("hostId") Long hostId);

    int insert(RoomRecordPO record);

    int insertSelective(RoomRecordPO record);

    RoomRecordPO selectByPrimaryKey(@Param("roomId") Long roomId, @Param("hostId") Long hostId);

    int updateByPrimaryKeySelective(RoomRecordPO record);

    int updateByPrimaryKey(RoomRecordPO record);
}