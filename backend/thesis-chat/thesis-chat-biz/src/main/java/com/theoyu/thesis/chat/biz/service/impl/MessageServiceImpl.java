package com.theoyu.thesis.chat.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.thesis.chat.biz.constants.MQConstants;
import com.theoyu.thesis.chat.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.chat.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.chat.biz.model.dto.MessageSendDTO;
import com.theoyu.thesis.chat.biz.model.entity.ConversationPO;
import com.theoyu.thesis.chat.biz.model.entity.ConversationParticipantPO;
import com.theoyu.thesis.chat.biz.model.entity.MessagePO;
import com.theoyu.thesis.chat.biz.model.mapper.ConversationPOMapper;
import com.theoyu.thesis.chat.biz.model.mapper.ConversationParticipantPOMapper;
import com.theoyu.thesis.chat.biz.model.mapper.MessagePOMapper;
import com.theoyu.thesis.chat.biz.model.vo.*;
import com.theoyu.thesis.chat.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.chat.biz.rpc.KVRpcService;
import com.theoyu.thesis.chat.biz.rpc.UserRpcService;
import com.theoyu.thesis.chat.biz.service.MessageService;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    
    private static final int MESSAGE_TYPE_TEXT = 1; // 文本消息
    private static final int MESSAGE_TYPE_IMAGE = 2; // 图片消息
    private static final int MESSAGE_TYPE_AUDIO = 3; // 语音消息
    private static final int MESSAGE_TYPE_VIDEO = 4; // 视频消息
    private static final int MESSAGE_TYPE_FILE = 6; // 文件消息


    // 消息发送频率限制：每个用户每秒最多发送3条消息
    private static final int MAX_MESSAGES_PER_SECOND = 3;
    @Resource
    private MessagePOMapper messagePOMapper;
    
    @Resource
    private ConversationPOMapper conversationPOMapper;
    
    @Resource
    private ConversationParticipantPOMapper participantPOMapper;
    
    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;
    
    @Resource
    private KVRpcService kvRpcService;
    
    @Resource
    private UserRpcService userRpcService;
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    /**
     * 发送消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> sendMessage(Long conversationId, SendMessageReqVO reqVO) {
        Long senderId = LoginUserContextHolder.getUserId();
        log.info("发送消息, conversationId: {}, senderId: {}, messageType: {}", 
            conversationId, senderId, reqVO.getMessageType());
        
        // 1. 参数校验
        validateMessageParams(reqVO);
        
        // 2. 频率限制检查
        checkRateLimit(senderId);
        
        // 3. 验证会话和权限
        validateConversationAccess(conversationId, senderId);
        
        // 4. 生成消息ID（Snowflake保证时序性，无需序列号）
        String messageIdStr = idGeneratorRpcService.getMessageSnowflakeId();
        Long messageId = Long.valueOf(messageIdStr);
        
        // 5. 处理消息内容
        String contentUuid = null;
        if (StringUtils.hasText(reqVO.getContent())) {
            contentUuid = kvRpcService.saveMessageContent(reqVO.getContent());
            if (contentUuid == null) {
                throw new BusinessException(ResponseCodeEnum.MESSAGE_CONTENT_SAVE_FAILED);
            }
        }
        // 6. 处理图片和视频URL
        String videoUri = null;
        String imgUris = null;

        if(reqVO.getMessageType() == MESSAGE_TYPE_IMAGE) {
            imgUris = org.apache.commons.lang3.StringUtils.join(reqVO.getImgUris(), ",");
        }else if(reqVO.getMessageType() == MESSAGE_TYPE_VIDEO) {
            videoUri = reqVO.getVideoUri();
        }
        
        // 7. 构建并保存消息
        LocalDateTime now = LocalDateTime.now();
        MessagePO message = MessagePO.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .messageType(reqVO.getMessageType())
                .contentUuid(contentUuid)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .createdTime(now)
                .isDeleted(false)
                .build();
        
        int insertResult = messagePOMapper.insertSelective(message);
        if (insertResult != 1) {
            log.error("保存消息失败, messageId: {}", messageId);
            throw new BusinessException(ResponseCodeEnum.MESSAGE_SEND_FAILED);
        }
        
        // 7. 更新会话的最后一条消息
        conversationPOMapper.updateLastMessage(conversationId, messageId, now, now);
        
        // 8. 更新其他参与者的未读数（排除发送者）
        participantPOMapper.batchIncrementUnreadCount(conversationId, senderId, now);
        // 推送在线用户
        // 9. 异步处理消息推送
        asyncSendToMQ(conversationId, message, reqVO.getContent());


        log.info("消息发送成功, messageId: {}, contentUuid: {}", messageId, contentUuid);
        return Response.success();
    }

    /**
     * 验证消息参数
     */
    private void validateMessageParams(SendMessageReqVO reqVO) {
        // 验证消息类型
        if (reqVO.getMessageType() < MESSAGE_TYPE_TEXT || reqVO.getMessageType() > MESSAGE_TYPE_FILE) {
            throw new BusinessException(ResponseCodeEnum.MESSAGE_TYPE_INVALID);
        }
        
        // 根据消息类型验证必填字段
        switch (reqVO.getMessageType()) {
            case MESSAGE_TYPE_TEXT:
                if (!StringUtils.hasText(reqVO.getContent())) {
                    throw new BusinessException(ResponseCodeEnum.MESSAGE_CONTENT_EMPTY);
                }
                break;
            case MESSAGE_TYPE_IMAGE:
                if (CollUtil.isEmpty(reqVO.getImgUris())) {
                    throw new BusinessException(ResponseCodeEnum.MESSAGE_IMG_URIS_REQUIRED);
                }
                break;
            case MESSAGE_TYPE_AUDIO:
                if (CollUtil.isEmpty(reqVO.getImgUris())) {
                    throw new BusinessException(ResponseCodeEnum.MESSAGE_AUDIO_CONTENT_REQUIRED);
                }
                break;            case MESSAGE_TYPE_VIDEO:
                if (!StringUtils.hasText(reqVO.getVideoUri())) {
                    throw new BusinessException(ResponseCodeEnum.MESSAGE_VIDEO_URI_REQUIRED);
                }
                break;
            case MESSAGE_TYPE_FILE:
                if (!StringUtils.hasText(reqVO.getContent())) {
                    throw new BusinessException(ResponseCodeEnum.MESSAGE_VIDEO_URI_REQUIRED);
                }
                break;
            default:
                break;
        }
    }
    
    /**
     * 频率限制检查
     */
    private void checkRateLimit(Long userId) {
        String rateLimitKey = RedisKeyConstants.buildRateLimitKey(userId);
        
        // 使用 Redis 的 INCR 命令实现计数
        Long count = redisTemplate.opsForValue().increment(rateLimitKey, 1);
        
        if (count == 1) {
            // 第一次访问，设置过期时间为1秒
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.SECONDS);
        }
        
        // 超过限制则抛出异常
        if (count != null && count > MAX_MESSAGES_PER_SECOND) {
            log.warn("用户发送消息频率超限, userId: {}, count: {}", userId, count);
            throw new BusinessException(ResponseCodeEnum.MESSAGE_TOO_FREQUENT);
        }
    }
    
    /**
     * 验证会话访问权限
     */
    private ConversationPO validateConversationAccess(Long conversationId, Long userId) {
        // 查询会话
        ConversationPO conversation = conversationPOMapper.selectByPrimaryKey(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_NOT_FOUND);
        }
        
        // 验证用户是否是参与者
        ConversationParticipantPO participant = participantPOMapper.selectByPrimaryKey(conversationId, userId);
        if (participant == null) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_ACCESS_DENIED);
        }
        
        // 验证会话是否激活
        if (!participant.getIsActive()) {
            throw new BusinessException(ResponseCodeEnum.CONVERSATION_NOT_ACTIVE);
        }
        
        return conversation;
    }
    
    /**
     * 异步发送 MQ 消息（仅用于离线推送）
     */
    private void asyncSendToMQ(Long conversationId, MessagePO message, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                List<ConversationParticipantPO> participants =
                        participantPOMapper.selectByConversationId(conversationId);

                List<Long> receiverIds = participants.stream()
                        .filter(p -> !p.getUserId().equals(message.getSenderId()) && p.getIsActive())
                        .map(ConversationParticipantPO::getUserId)
                        .collect(Collectors.toList());

                if (receiverIds.isEmpty()) {
                    return;
                }

                MessageSendDTO mqMessage = MessageSendDTO.builder()
                        .messageId(message.getId())
                        .conversationId(conversationId)
                        .senderId(message.getSenderId())
                        .receiverIds(receiverIds)
                        .messageType(message.getMessageType())
                        .content(content)
                        .imgUris(parseImgUris(message.getImgUris()))
                        .videoUri(message.getVideoUri())
                        .sendTime(message.getCreatedTime())
                        .build();
                // 广播消费保证每个 chat 实例都尝试向本机 WebSocket 会话推送。
                rocketMQTemplate.syncSend(
                        MQConstants.TOPIC_MESSAGE_SEND + ":" + MQConstants.TAG_MESSAGE_SEND,
                        mqMessage,
                        3000
                );

                log.info("==> MQ消息发送成功, messageId: {}", message.getId());

            } catch (Exception e) {
                log.error("==> MQ消息发送失败, messageId: {}", message.getId(), e);
            }
        }, threadPoolTaskExecutor);
    }
    private List<String> parseImgUris(String imgUris) {
        if (StringUtils.hasText(imgUris)) {
            return Arrays.asList(imgUris.split(","));
        }
        return null;
    }


    /**
     * 获取消息列表（游标分页）
     */
    @Override
    public Response<GetMessagesResVO> getMessages(Long conversationId, GetMessagesReqVO reqVO) {
        Long currentUserId = LoginUserContextHolder.getUserId();
        log.info("获取消息列表, conversationId: {}, userId: {}, cursor: {}, limit: {}",
                conversationId, currentUserId, reqVO.getCursor(), reqVO.getLimit());

         validateConversationAccess(conversationId, currentUserId);

        // 从数据库查询
        List<MessagePO> messagePOs = messagePOMapper.selectByConversationIdWithCursor(
                conversationId,
                reqVO.getCursor(),
                reqVO.getLimit() + 1
        );

        boolean hasMore = messagePOs.size() > reqVO.getLimit();
        if (hasMore) {
            messagePOs = messagePOs.subList(0, reqVO.getLimit());
        }

        if (CollUtil.isEmpty(messagePOs)) {
            return Response.success(GetMessagesResVO.builder()
                    .messages(new ArrayList<>())
                    .nextCursor(null)
                    .hasMore(false)
                    .build());
        }

        // 异步批量查询：用户信息、文本内容
        CompletableFuture<Map<Long, FindUserByIdRspDTO>> userInfoFuture =
                asyncBatchGetUserInfo(messagePOs);

        CompletableFuture<Map<String, String>> contentFuture =
                asyncBatchGetMessageContent(messagePOs);

        // 等待异步任务完成
        CompletableFuture.allOf(userInfoFuture, contentFuture).join();

        Map<Long, FindUserByIdRspDTO> userInfoMap = userInfoFuture.join();
        Map<String, String> contentMap = contentFuture.join();

        // 构建消息VO列表
        List<MessageVO> messageVOs = messagePOs.stream()
                .map(po -> buildMessageVO(po, currentUserId, userInfoMap, contentMap))
                .collect(Collectors.toList());

        Long nextCursor = hasMore ? messagePOs.get(messagePOs.size() - 1).getId() : null;

        GetMessagesResVO result = GetMessagesResVO.builder()
                .messages(messageVOs)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();


        log.info("获取消息列表成功, conversationId: {}, messageCount: {}, hasMore: {}",
                conversationId, messageVOs.size(), hasMore);

        return Response.success(result);
    }
    /**
     * 异步批量获取用户信息
     * 优化：直接调用 UserRpcService 的批量查询接口（内置缓存机制）
     */
    private CompletableFuture<Map<Long, FindUserByIdRspDTO>> asyncBatchGetUserInfo(List<MessagePO> messagePOs) {
        return CompletableFuture.supplyAsync(() -> {
            // 收集所有发送者ID
            Set<Long> senderIds = messagePOs.stream()
                    .map(MessagePO::getSenderId)
                    .collect(Collectors.toSet());
            // 直接调用批量查询接口
            Map<Long, FindUserByIdRspDTO> userInfoMap = userRpcService.findByIds2(senderIds);

            log.debug("批量查询用户信息, 请求数: {}, 成功数: {}", senderIds.size(), userInfoMap.size());
            return userInfoMap;
        }, threadPoolTaskExecutor);
    }

    /**
     * 异步批量获取消息文本内容
     */
    private CompletableFuture<Map<String, String>> asyncBatchGetMessageContent(List<MessagePO> messagePOs) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> contentUuids = messagePOs.stream()
                    .map(MessagePO::getContentUuid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (CollUtil.isEmpty(contentUuids)) {
                return new HashMap<>();
            }

            return kvRpcService.batchGetMessageContent(contentUuids);
        }, threadPoolTaskExecutor);
    }

    /**
     * 构建消息VO
     */
    private MessageVO buildMessageVO(MessagePO po,
                                     Long currentUserId,
                                     Map<Long, FindUserByIdRspDTO> userInfoMap,
                                     Map<String, String> contentMap) {
        FindUserByIdRspDTO senderInfo = userInfoMap.get(po.getSenderId());

        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .senderId(po.getSenderId())
                .senderNickname(senderInfo.getNickName())
                .senderAvatar(senderInfo.getAvatar())
                .messageType(po.getMessageType())
                .videoUri(po.getVideoUri())
                .createdTime(po.getCreatedTime())
                .isSelf(po.getSenderId().equals(currentUserId));

        // 处理文本内容
        if (MESSAGE_TYPE_TEXT == po.getMessageType() && po.getContentUuid() != null) {
            builder.content(contentMap.get(po.getContentUuid()));
        }

        // 处理图片URL列表
        if (StringUtils.hasText(po.getImgUris())) {
            try {
                List<String> imgUris = Arrays.asList(po.getImgUris().split(","));
                builder.imgUris(imgUris);
            } catch (Exception e) {
                log.error("解析图片URL失败, messageId: {}", po.getId(), e);
            }
        }

        return builder.build();
    }

    

}
