package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.SfuNodePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SfuNodePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SfuNodePO record);

    int insertSelective(SfuNodePO record);

    SfuNodePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SfuNodePO record);

    int updateByPrimaryKey(SfuNodePO record);

    SfuNodePO selectByInstanceId(@Param("instanceId") String instanceId);

    List<SfuNodePO> selectAvailableNodes();

    int markMissingInstancesOffline(@Param("instanceIds") List<String> instanceIds);

    int markAllNodesOffline();

    int incrementCurrentLoad(@Param("id") Long id);

    int decrementCurrentLoad(@Param("id") Long id);
}
