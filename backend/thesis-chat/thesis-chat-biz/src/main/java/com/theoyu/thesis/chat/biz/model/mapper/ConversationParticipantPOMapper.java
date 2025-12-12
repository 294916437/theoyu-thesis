package com.theoyu.thesis.chat.biz.model.mapper;

import com.theoyu.thesis.chat.biz.model.entity.ConversationParticipantPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationParticipantPOMapper {
    int deleteByPrimaryKey(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    int insert(ConversationParticipantPO record);

    int insertSelective(ConversationParticipantPO record);

    ConversationParticipantPO selectByPrimaryKey(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    int updateByPrimaryKeySelective(ConversationParticipantPO record);

    int updateByPrimaryKey(ConversationParticipantPO record);

    /**
     * 查询会话的所有参与者
     */
    List<ConversationParticipantPO> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 批量查询会话的指定用户参与信息
     */
    List<ConversationParticipantPO> selectByConversationIdsAndUserId(@Param("conversationIds") List<Long> conversationIds,
                                                                     @Param("userId") Long userId);


    /**
     * 批量查询会话的所有参与者
     */
    List<ConversationParticipantPO> selectByConversationIds(@Param("conversationIds") List<Long> conversationIds);


    /**
     * 批量插入参与者
     */
    int batchInsert(@Param("participants") List<ConversationParticipantPO> participants);
    /**
     * 软删除参与者（设置为不活跃）
     */
    int softDeleteParticipant(@Param("conversationId") Long conversationId,
                              @Param("userId") Long userId,

                              @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 统计会话的活跃参与者数量
     */
    int countActiveParticipants(@Param("conversationId") Long conversationId);
    /**
     * 批量增加未读数（排除指定用户）
     */
    int batchIncrementUnreadCount(@Param("conversationId") Long conversationId,
                                  @Param("excludeUserId") Long excludeUserId,
                                  @Param("updatedTime") LocalDateTime updatedTime);
}