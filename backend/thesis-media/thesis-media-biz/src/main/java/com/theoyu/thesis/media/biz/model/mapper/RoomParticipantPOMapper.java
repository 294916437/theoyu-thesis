package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;

public interface RoomParticipantPOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomParticipantPO record);

    int insertSelective(RoomParticipantPO record);

    RoomParticipantPO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoomParticipantPO record);

    int updateByPrimaryKey(RoomParticipantPO record);
}