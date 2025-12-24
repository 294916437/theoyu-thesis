package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomPOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomPO record);

    int insertSelective(RoomPO record);

    RoomPO selectByPrimaryKey(Long id);

    List<RoomPO> selectByIds(@Param("ids") List<Long> ids);

    int updateByPrimaryKeySelective(RoomPO record);

    int updateByPrimaryKeyWithBLOBs(RoomPO record);

    int updateByPrimaryKey(RoomPO record);
    /**
     * 根据用户ID统计活跃房间数
     */
    Integer countActiveRoomsByHostId(@Param("hostId") Long hostId);

    /**
     * 根据房间号查询房间
     */
    RoomPO selectByRoomNo(@Param("roomNo") String roomNo);

    /**
     * 更新房间状态
     */
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 查询用户即将开始的会议
     */
    List<RoomPO> selectUpcomingRoomsByUserId(@Param("userId") Long userId,@Param("offset") Long offset,@Param("pageSize") Long pageSize);



}