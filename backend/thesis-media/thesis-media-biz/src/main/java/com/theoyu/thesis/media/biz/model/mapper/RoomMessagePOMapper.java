package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;
import org.apache.ibatis.annotations.Param;
import java.util.List;


public interface RoomMessagePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomMessagePO record);

    int insertSelective(RoomMessagePO record);

    RoomMessagePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoomMessagePO record);

    int updateByPrimaryKey(RoomMessagePO record);

    /**
     * 分页查询房间消息
     */
    List<RoomMessagePO> selectByRoomId(@Param("roomId") Long roomId,
                                       @Param("messageType") Integer messageType,
                                       @Param("contentType") Integer contentType,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

}