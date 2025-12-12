package com.theoyu.thesis.chat.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.thesis.chat.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.chat.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.chat.biz.model.entity.ConversationPO;
import com.theoyu.thesis.chat.biz.model.entity.ConversationParticipantPO;
import com.theoyu.thesis.chat.biz.model.mapper.ConversationPOMapper;
import com.theoyu.thesis.chat.biz.model.mapper.ConversationParticipantPOMapper;
import com.theoyu.thesis.chat.biz.model.vo.*;
import com.theoyu.thesis.chat.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.chat.biz.rpc.UserRpcService;
import com.theoyu.thesis.chat.biz.service.ConversationService;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    
    private static final long CACHE_EXPIRE_TIME = 30; // 缓存30分钟
    private static final long DETAIL_CACHE_EXPIRE_TIME = 10; // 详情缓存10分钟
    private static final int PAGE_SIZE = 20; // 每页数量
    private static final int CONVERSATION_TYPE_PRIVATE = 1; // 私聊类型
    private static final int CONVERSATION_TYPE_GROUP = 2; // 群聊类型


    @Resource
    private ConversationPOMapper conversationPOMapper;
    
    @Resource
    private ConversationParticipantPOMapper participantPOMapper;
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;

    @Override
    public Response<ConversationListResVO> getConversationList(ConversationListReqVO reqVO) {
        Long userId = reqVO.getUserId();
        log.info("获取会话列表, userId: {}, cursor: {}", userId, reqVO.getCursor());

        // 单次查询数量，多查一条，用于判断是否还有更多数据
        int limit = PAGE_SIZE + 1;
        
        // 查询会话列表
        List<ConversationPO> conversations = conversationPOMapper.selectByUserIdWithCursor(
            userId, 
            CONVERSATION_TYPE_PRIVATE,
            reqVO.getCursor(), 
            limit
        );
        
        if (CollUtil.isEmpty(conversations)) {
            return Response.success(buildEmptyListResult());
        }
        
        // 判断是否还有更多数据
        boolean hasMore = conversations.size() > PAGE_SIZE;
        if (hasMore) {
            conversations = conversations.subList(0, PAGE_SIZE);
        }
        
        // 计算下一页游标
        Long nextCursor = hasMore ? conversations.get(conversations.size() - 1).getId() : null;
        
        // 提取会话ID列表
        List<Long> conversationIds = conversations.stream()
                .map(ConversationPO::getId)
                .collect(Collectors.toList());
        
        // 批量查询当前用户的参与者信息（用于获取未读数）
        List<ConversationParticipantPO> currentUserParticipants = 
            participantPOMapper.selectByConversationIdsAndUserId(conversationIds, userId);
        
        Map<Long, ConversationParticipantPO> currentUserParticipantMap = currentUserParticipants.stream()
                .collect(Collectors.toMap(
                    ConversationParticipantPO::getConversationId, 
                    p -> p,
                    (existing, replacement) -> existing
                ));
        
        // 批量查询所有参与者
        List<ConversationParticipantPO> allParticipants = 
            participantPOMapper.selectByConversationIds(conversationIds);
        

        // 按会话ID分组，找出每个会话中的对方用户ID
        Map<Long, Long> conversationToTargetUserMap = new HashMap<>();
        
        for (ConversationParticipantPO participant : allParticipants) {
            Long conversationId = participant.getConversationId();
            Long participantUserId = participant.getUserId();
            
            // 只保存对方用户ID（排除当前用户）
            if (!participantUserId.equals(userId)) {
                conversationToTargetUserMap.put(conversationId, participantUserId);
            }
        }
        
        // 提取所有对方用户ID
        List<Long> targetUserIds = new ArrayList<>(conversationToTargetUserMap.values());
        
        if (targetUserIds.isEmpty()) {
            log.warn("⚠️ 未找到任何对方用户ID，可能数据异常");
        }
        
        // 批量查询用户信息（RPC调用）
        Map<Long, FindUserByIdRspDTO> userMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(targetUserIds)) {
            userMap = userRpcService.findByIds2(targetUserIds);
        }
        
        // 组装返回结果
        Map<Long, FindUserByIdRspDTO> finalUserMap = userMap;
        List<ConversationListResVO.ConversationItemVO> items = conversations.stream()
                .map(conv -> {
                    ConversationListResVO.ConversationItemVO item = new ConversationListResVO.ConversationItemVO();
                    item.setId(conv.getId());
                    item.setConversationType(conv.getConversationType());
                    item.setTitle(conv.getTitle());
                    item.setLastMessageId(conv.getLastMessageId());
                    item.setLastMessageTime(conv.getLastMessageTime());
                    
                    // 填充当前用户的参与者信息（未读数等）
                    ConversationParticipantPO currentParticipant = currentUserParticipantMap.get(conv.getId());
                    item.setUnreadCount(currentParticipant != null ? currentParticipant.getUnreadCount() : 0);
                    item.setIsActive(currentParticipant != null ? currentParticipant.getIsActive() : false);
                    
                    // 填充对方用户信息f
                    Long targetUserId = conversationToTargetUserMap.get(conv.getId());
                    if (targetUserId != null) {
                        FindUserByIdRspDTO userInfo = finalUserMap.get(targetUserId);
                        if (userInfo != null) {
                            ConversationListResVO.UserVO userVO = ConversationListResVO.UserVO.builder()
                                    .userId(targetUserId)
                                    .nickname(userInfo.getNickName())
                                    .avatar(userInfo.getAvatar())
                                    .build();
                            item.setUser(userVO);
                            
                            log.debug("会话 {} 的对方用户信息: userId={}, nickname={}", 
                                conv.getId(), targetUserId, userInfo.getNickName());
                        } else {
                            log.warn("未找到用户信息, userId: {}", targetUserId);
                            item.setUser(buildDefaultUserVO(targetUserId));
                        }
                    } else {
                        log.warn("会话 {} 未找到对方用户ID", conv.getId());
                        item.setUser(null);
                    }
                    
                    return item;
                })
                .collect(Collectors.toList());
        
        ConversationListResVO result = new ConversationListResVO();
        result.setConversations(items);
        result.setHasMore(hasMore);
        result.setNextCursor(nextCursor);
        
        return Response.success(result);
    }

    /**
     * 构建默认用户VO（当用户信息查询失败时使用）
     */
    private ConversationListResVO.UserVO buildDefaultUserVO(Long userId) {
        return ConversationListResVO.UserVO.builder()
                .userId(userId)
                .nickname("未知用户")
                .avatar(null)
                .build();
    }
    
    @Override
    public Response<ConversationDetailResVO> getConversationDetail(Long conversationId) {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("获取会话详情, userId: {}, conversationId: {}", userId, conversationId);
        
        // 尝试从缓存获取会话详情
        String detailCacheKey = RedisKeyConstants.CONVERSATION_DETAIL_CACHE_KEY + conversationId + ":" + userId;
        String cachedDetailJson = redisTemplate.opsForValue().get(detailCacheKey);
        
        if (cachedDetailJson != null) {
            log.info("从缓存获取会话详情成功");
            return Response.success(JSON.parseObject(cachedDetailJson, ConversationDetailResVO.class));
        }
        
        // 并行查询会话基本信息、当前用户参与信息、所有参与者信息
        CompletableFuture<ConversationPO> conversationFuture = CompletableFuture.supplyAsync(() -> 
            getConversationFromCacheOrDB(conversationId), threadPoolTaskExecutor);
        
        CompletableFuture<ConversationParticipantPO> currentUserParticipantFuture = 
            CompletableFuture.supplyAsync(() -> 
                participantPOMapper.selectByPrimaryKey(conversationId, userId), threadPoolTaskExecutor);
        
        CompletableFuture<List<ConversationParticipantPO>> allParticipantsFuture = 
            CompletableFuture.supplyAsync(() -> 
                participantPOMapper.selectByConversationId(conversationId), threadPoolTaskExecutor);
        
        // 等待所有并行查询完成
        CompletableFuture.allOf(conversationFuture, currentUserParticipantFuture, allParticipantsFuture).join();
        
        ConversationPO conversation = conversationFuture.join();
        ConversationParticipantPO currentUserParticipant = currentUserParticipantFuture.join();
        List<ConversationParticipantPO> allParticipants = allParticipantsFuture.join();
        
        // 验证会话
        validateConversationAccess(conversation, currentUserParticipant);
        
        // 批量查询用户信息
        List<Long> userIds = allParticipants.stream()
                .map(ConversationParticipantPO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, FindUserByIdRspDTO> userMap = userRpcService.findByIds2(userIds);
        
        // 构建参与者信息列表 - 批量处理
        List<ConversationDetailResVO.ParticipantVO> participantVOs = allParticipants.stream()
                .map(p -> {
                    ConversationDetailResVO.ParticipantVO vo = new ConversationDetailResVO.ParticipantVO();
                    vo.setUserId(p.getUserId());
                    vo.setIsActive(p.getIsActive());
                    vo.setJoinedTime(p.getJoinedTime());
                    
                    // 从批量查询的结果中获取用户信息
                    FindUserByIdRspDTO user = userMap.get(p.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickName());
                        vo.setAvatar(user.getAvatar());
                    }
                    
                    return vo;
                })
                .collect(Collectors.toList());
        
        // 组装返回结果
        ConversationDetailResVO result = buildConversationDetailResult(
            conversation, currentUserParticipant, participantVOs);
        
        // 缓存会话详情
        redisTemplate.opsForValue().set(detailCacheKey, JSON.toJSONString(result), 
            DETAIL_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return Response.success(result);
    }

    /**
     * 创建会话，仅限于私聊
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<CreateConversationResVO> createConversation(CreateConversationReqVO reqVO) {
        Long currentUserId = LoginUserContextHolder.getUserId();
        Long targetUserId = reqVO.getTargetUserId();

        // 1. 参数校验
        if(currentUserId.equals(targetUserId)) {
            throw new BusinessException(ResponseCodeEnum.CANNOT_CHAT_WITH_SELF);
        }

        // 2. 检查是否已存在私聊会话
        ConversationPO existingConversation = conversationPOMapper.selectPrivateConversationByUserIds(currentUserId, targetUserId);

        if(existingConversation!=null) {
            log.info("私聊会话已经存在，conversationId: {}", existingConversation.getId());
            return Response.success(buildConversationResponse(existingConversation,targetUserId,false));
        }

        // 3. 创建新会话
        String conversationIdStr = idGeneratorRpcService.getConversationSnowflakeId();
        Long conversationId = Long.parseLong(conversationIdStr);

        LocalDateTime now = LocalDateTime.now();

        ConversationPO conversation = ConversationPO.builder()
                .id(conversationId)
                .conversationType(CONVERSATION_TYPE_PRIVATE)
                .title(null)
                .createdTime(now)
                .updatedTime(now)
                .build();
        int conversationInsertResult = conversationPOMapper.insertSelective(conversation);
        if (conversationInsertResult != 1) {
            log.error("创建会话记录失败, conversationId: {}", conversationId);
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_CREATE_FAILED);

        }


        // 6. 批量创建参与者记录（当前用户和对方用户）
        List<ConversationParticipantPO> participants = Arrays.asList(
                buildParticipant(conversationId, currentUserId, now),
                buildParticipant(conversationId, targetUserId, now)
        );

        int participantInsertResult = participantPOMapper.batchInsert(participants);

        if (participantInsertResult != 2) {
            log.error("创建参与者记录失败, conversationId: {}, insertCount: {}",
                    conversationId, participantInsertResult);
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_CREATE_FAILED);
        }

        // 7. 缓存新创建的会话
        cacheConversation(conversationId, conversation);

        // 8. 异步清除双方用户的会话列表缓存
        asyncInvalidateConversationListCache(Arrays.asList(currentUserId, targetUserId));

        // 9. 构建返回结果
        CreateConversationResVO result = buildConversationResponse(conversation, targetUserId, true);

        log.info("创建私聊会话成功, conversationId: {}", conversationId);
        return Response.success(result);
    }

    /**
     * 退出/删除会话
     * 对于私聊：软删除参与者记录（设置 is_active = 0）
     * 对于群聊，TODO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> leaveConversation(Long conversationId) {
        Long currentUserId = LoginUserContextHolder.getUserId();
        log.info("用户退出会话, userId: {}, conversationId: {}", currentUserId, conversationId);

        // 1. 验证会话是否存在
        ConversationPO conversation = conversationPOMapper.selectByPrimaryKey(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_NOT_FOUND);
        }

        // 2. 验证用户是否是会话参与者
        ConversationParticipantPO participant = participantPOMapper.selectByPrimaryKey(conversationId, currentUserId);
        if (participant == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_ACCESS_DENIED);
        }

        // 3. 检查是否已经退出
        if (!participant.getIsActive()) {
            log.warn("用户已经退出该会话, userId: {}, conversationId: {}", currentUserId, conversationId);
            return Response.success();
        }

        // 4. 软删除参与者记录
        LocalDateTime now = LocalDateTime.now();
        int updateResult = participantPOMapper.softDeleteParticipant(conversationId, currentUserId, now);

        if (updateResult != 1) {
            log.error("退出会话失败, userId: {}, conversationId: {}", currentUserId, conversationId);
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        // 5. 异步处理缓存清理和后续操作
        asyncHandleLeaveConversation(conversationId, currentUserId);

        log.info("用户成功退出会话, userId: {}, conversationId: {}", currentUserId, conversationId);
        return Response.success();
    }
    /**
     * 异步处理退出会话后的操作
     */
    private void asyncHandleLeaveConversation(Long conversationId, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 清除用户的会话列表缓存
                invalidateUserConversationListCache(userId);

                // 2. 清除会话详情缓存
                invalidateConversationDetailCache(conversationId, userId);

                // 3. 检查是否所有参与者都已退出
                int activeCount = participantPOMapper.countActiveParticipants(conversationId);

                // 如果所有参与者都已退出，可以考虑物理删除会话（可选）
                if (activeCount == 0) {
                    log.info("会话所有参与者已退出, conversationId: {}, 可以考虑归档或删除", conversationId);
                    // TODO: 可以发送MQ消息进行会话归档
                    // rocketMQTemplate.syncSend("conversation-archive", conversationId);
                }

            } catch (Exception e) {
                log.error("异步处理退出会话失败, conversationId: {}, userId: {}", conversationId, userId, e);
            }
        }, threadPoolTaskExecutor);
    }
    /**
     * 清除用户的会话列表缓存
     */
    private void invalidateUserConversationListCache(Long userId) {
        String listCachePattern = RedisKeyConstants.CONVERSATION_LIST_CACHE_KEY + userId + ":*";
        Set<String> keys = redisTemplate.keys(listCachePattern);
        if (CollUtil.isNotEmpty(keys)) {
            redisTemplate.delete(keys);
            log.info("清除用户会话列表缓存, userId: {}, cacheCount: {}", userId, keys.size());
        }
    }

    /**
     * 清除会话详情缓存
     */
    private void invalidateConversationDetailCache(Long conversationId, Long userId) {
        // 清除当前用户的会话详情缓存
        String detailCacheKey = RedisKeyConstants.CONVERSATION_DETAIL_CACHE_KEY + conversationId + ":" + userId;
        redisTemplate.delete(detailCacheKey);

        // 清除会话基本信息缓存
        String conversationCacheKey = RedisKeyConstants.CONVERSATION_CACHE_KEY + conversationId;
        redisTemplate.delete(conversationCacheKey);

        log.info("清除会话详情缓存, conversationId: {}, userId: {}", conversationId, userId);
    }

    /**
     * 构建参与者记录
     */
    private ConversationParticipantPO buildParticipant(Long conversationId, Long userId, LocalDateTime now) {
        return ConversationParticipantPO.builder()
                .conversationId(conversationId)
                .userId(userId)
                .isActive(true)
                .unreadCount(0)
                .joinedTime(now)
                .updatedTime(now)
                .build();
    }


    /**
     * 缓存会话信息
     */
    private void cacheConversation(Long conversationId, ConversationPO conversation) {
        String cacheKey = RedisKeyConstants.CONVERSATION_CACHE_KEY + conversationId;
        redisTemplate.opsForValue().set(
                cacheKey,
                JSON.toJSONString(conversation),
                CACHE_EXPIRE_TIME,
                TimeUnit.MINUTES
        );
    }

    /**
     * 异步清除会话列表缓存
     */
    private void asyncInvalidateConversationListCache(List<Long> userIds) {
        CompletableFuture.runAsync(() -> {
            userIds.forEach(userId -> {
                String listCachePattern = RedisKeyConstants.CONVERSATION_LIST_CACHE_KEY + userId + ":*";
                Set<String> keys = redisTemplate.keys(listCachePattern);
                if (CollUtil.isNotEmpty(keys)) {
                    redisTemplate.delete(keys);
                    log.info("清除用户会话列表缓存, userId: {}, cacheCount: {}", userId, keys.size());
                }
            });
        }, threadPoolTaskExecutor).exceptionally(e -> {
            log.error("清除会话列表缓存失败", e);
            return null;
        });
    }

    /**
     * 构建会话响应结果
     */
    private CreateConversationResVO buildConversationResponse(
            ConversationPO conversation,
            Long targetUserId,
            boolean isNew) {

        CreateConversationResVO result = new CreateConversationResVO();
        result.setConversationId(conversation.getId());
        result.setConversationType(CONVERSATION_TYPE_PRIVATE);
        result.setTitle(null);
        result.setTargetUserId(targetUserId);
        result.setIsNew(isNew);

        return result;
    }

    /**
     * 构建空的会话列表结果
     */
    private ConversationListResVO buildEmptyListResult() {
        ConversationListResVO emptyResult = new ConversationListResVO();
        emptyResult.setConversations(Collections.emptyList());
        emptyResult.setHasMore(false);
        emptyResult.setNextCursor(null);
        return emptyResult;
    }

    /**
     * 从缓存或数据库获取会话信息
     */
    private ConversationPO getConversationFromCacheOrDB(Long conversationId) {
        String cacheKey = RedisKeyConstants.CONVERSATION_CACHE_KEY + conversationId;
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedJson != null) {
            return JSON.parseObject(cachedJson, ConversationPO.class);
        }
        
        ConversationPO conversation = conversationPOMapper.selectByPrimaryKey(conversationId);
        if (conversation != null) {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(conversation), 
                CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        return conversation;
    }

    /**
     * 验证会话访问权限
     */
    private void validateConversationAccess(ConversationPO conversation, 
                                           ConversationParticipantPO participant) {
        if (conversation == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_NOT_FOUND);
        }
        
        if (participant == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_ACCESS_DENIED);
        }
        
        if (!participant.getIsActive()) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_NOT_ACTIVE);
        }
    }

    /**
     * 构建会话详情结果
     */
    private ConversationDetailResVO buildConversationDetailResult(
            ConversationPO conversation,
            ConversationParticipantPO currentUserParticipant,
            List<ConversationDetailResVO.ParticipantVO> participantVOs) {
        
        ConversationDetailResVO result = new ConversationDetailResVO();
        result.setId(conversation.getId());
        result.setConversationType(conversation.getConversationType());
        result.setTitle(conversation.getTitle());
        result.setLastMessageId(conversation.getLastMessageId());
        result.setLastMessageTime(conversation.getLastMessageTime());
        result.setUnreadCount(currentUserParticipant.getUnreadCount());
        result.setParticipants(participantVOs);
        result.setCreatedTime(conversation.getCreatedTime());
        
        return result;
    }
}