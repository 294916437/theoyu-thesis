package com.theoyu.thesis.user.biz.model.mapper;

import com.theoyu.thesis.user.biz.model.entity.UserRoleRelPO;

public interface UserRoleRelPOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserRoleRelPO record);

    int insertSelective(UserRoleRelPO record);

    UserRoleRelPO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRoleRelPO record);

    int updateByPrimaryKey(UserRoleRelPO record);
}