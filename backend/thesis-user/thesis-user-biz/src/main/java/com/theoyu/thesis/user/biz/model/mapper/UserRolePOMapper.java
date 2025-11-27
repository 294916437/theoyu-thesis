package com.theoyu.thesis.user.biz.model.mapper;

import com.theoyu.thesis.user.biz.model.entity.UserRolePO;

public interface UserRolePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserRolePO record);

    int insertSelective(UserRolePO record);

    UserRolePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRolePO record);

    int updateByPrimaryKey(UserRolePO record);
}