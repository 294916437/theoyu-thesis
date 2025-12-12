package com.theoyu.thesis.chat.biz.model.mapper;

import com.theoyu.thesis.chat.biz.model.entity.ConversationPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationPOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConversationPO record);

    int insertSelective(ConversationPO record);

    ConversationPO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConversationPO record);

    int updateByPrimaryKey(ConversationPO record);

    /**
     * 基于游标分页查询用户会话列表
     * 按照最后消息时间倒序排列
     */
    List<ConversationPO> selectByUserIdWithCursor(@Param("userId") Long userId,
                                                  @Param("conversationType") Integer conversationType,
                                                  @Param("cursor") Long cursor,
                                                  @Param("limit") Integer limit);
    /**
     * 查询两个用户之间是否已存在单聊会话
     */
    ConversationPO selectPrivateConversationByUserIds(@Param("userId1") Long userId1,
                                                      @Param("userId2") Long userId2);

    /**
     * 更新会话的最后一条消息信息
     */
    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessageId") Long lastMessageId,
                          @Param("lastMessageTime") LocalDateTime lastMessageTime,
                          @Param("updatedTime") LocalDateTime updatedTime);
}