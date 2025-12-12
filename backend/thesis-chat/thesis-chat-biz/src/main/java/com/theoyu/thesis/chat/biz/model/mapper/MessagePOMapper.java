package com.theoyu.thesis.chat.biz.model.mapper;

import com.theoyu.thesis.chat.biz.model.entity.MessagePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessagePOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(MessagePO record);

    int insertSelective(MessagePO record);

    MessagePO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(MessagePO record);

    int updateByPrimaryKey(MessagePO record);

    /**
     * 基于游标分页查询消息列表
     * 按消息ID倒序（Snowflake ID保证时序性）
     *
     * @param conversationId 会话ID
     * @param cursor 游标（消息ID），为null时查询最新消息
     * @param limit 每页数量
     * @return 消息列表
     */
    List<MessagePO> selectByConversationIdWithCursor(@Param("conversationId") Long conversationId,
                                                     @Param("cursor") Long cursor,
                                                     @Param("limit") Integer limit);

    /**
     * 统计会话的消息总数
     *
     * @param conversationId 会话ID
     * @return 消息总数
     */
    Long countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 批量查询消息（用于查询回复的消息）
     *
     * @param ids 消息ID列表
     * @return 消息列表
     */
    List<MessagePO> selectByIds(@Param("ids") List<Long> ids);
}