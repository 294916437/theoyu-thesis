package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;

public interface RoomMessagePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomMessagePO record);

    int insertSelective(RoomMessagePO record);

    RoomMessagePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoomMessagePO record);

    int updateByPrimaryKey(RoomMessagePO record);
}