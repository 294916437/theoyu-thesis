package com.theoyu.thesis.media.biz.model.mapper;

import com.theoyu.thesis.media.biz.model.entity.SfuNodePO;

public interface SfuNodePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SfuNodePO record);

    int insertSelective(SfuNodePO record);

    SfuNodePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SfuNodePO record);

    int updateByPrimaryKey(SfuNodePO record);
}