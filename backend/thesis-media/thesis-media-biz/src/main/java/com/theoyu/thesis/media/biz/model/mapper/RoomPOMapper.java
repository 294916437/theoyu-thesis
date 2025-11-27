package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomPO;

public interface RoomPOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomPO record);

    int insertSelective(RoomPO record);

    RoomPO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoomPO record);

    int updateByPrimaryKeyWithBLOBs(RoomPO record);

    int updateByPrimaryKey(RoomPO record);
}