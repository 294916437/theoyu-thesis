package com.theoyu.thesis.media.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageReqVO;
import com.theoyu.thesis.media.biz.model.vo.RoomMessageResVO;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import com.theoyu.thesis.media.biz.model.mapper.RoomMessagePOMapper;
import com.theoyu.thesis.media.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.media.biz.rpc.UserRpcService;
import com.theoyu.thesis.media.biz.service.RoomMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoomMessageServiceImpl implements RoomMessageService {
    @Resource
    private RoomMessagePOMapper roomMessagePOMapper;

    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private static final Integer MAX_CACHE_MESSAGE_SIZE = 100;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomMessageResVO sendMessage(RoomMessageReqVO reqVO) {
        Long userId = reqVO.getUserId();
        Long roomId = reqVO.getRoomId();


        // 校验消息内容
        if (!StringUtils.hasText(reqVO.getContent())) {
            throw new BusinessException(ResponseCodeEnum.MESSAGE_CONTENT_EMPTY);
        }

        // 生成消息ID
        Long messageId = Long.valueOf(idGeneratorRpcService.getRoomMsgId());
        long currentTime = System.currentTimeMillis();

        // 构建消息PO
        LocalDateTime now = LocalDateTime.now();
        RoomMessagePO messagePO = RoomMessagePO.builder()
                .id(messageId)
                .roomId(roomId)
                .senderId(userId)
                .content(reqVO.getContent())
                .messageType(reqVO.getMessageType())
                .contentType(reqVO.getContentType())
                .createdTime(now)
                .updatedTime(now)
                .build();

        // 保存到数据库
        int result = roomMessagePOMapper.insert(messagePO);
        if (result <= 0) {
            log.error("消息保存失败, messageId: {}", messageId);
            throw new BusinessException(ResponseCodeEnum.MESSAGE_SEND_FAILED);
        }

        log.info("消息保存成功, messageId: {}, roomId: {}, senderId: {}", messageId, roomId, userId);

        // 异步处理
        threadPoolTaskExecutor.execute(() -> {
            // 缓存到Redis
            cacheMessageToRedis(messagePO, currentTime);

            // 发送MQ消息
            sendMessageToMQ(messagePO, currentTime);
        });

        // 构建响应VO
        RoomMessageResVO resVO = buildMessageResVO(messagePO);

        // 广播消息到房间订阅者
        messagingTemplate.convertAndSend("/topic/room/" + roomId, resVO);
        log.info("消息已广播到房间, roomId: {}, messageId: {}", roomId, messageId);

        return resVO;
    }

    @Override
    public List<RoomMessageResVO> getMessageHistory(Long roomId, Integer pageNum, Integer pageSize) {
        // 先从Redis获取最近消息
        String redisKey = String.format(RedisKeyConstants.ROOM_MESSAGE_KEY, roomId);
        Set<ZSetOperations.TypedTuple<Object>> cachedMessages = redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKey, (pageNum - 1) * pageSize, pageNum * pageSize - 1);

        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            log.info("从Redis获取房间消息, roomId: {}, size: {}", roomId, cachedMessages.size());
            return cachedMessages.stream()
                    .map(tuple -> JsonUtils.parseObject((String) tuple.getValue(), RoomMessageResVO.class))
                    .collect(Collectors.toList());
        }

        // Redis未命中,从数据库查询
        log.info("从数据库查询房间消息, roomId: {}", roomId);
        Integer offset = (pageNum - 1) * pageSize;
        List<RoomMessagePO> messageList = roomMessagePOMapper.selectByRoomId(roomId, offset, pageSize);

        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户信息
        Set<Long> userIds = messageList.stream()
                .map(RoomMessagePO::getSenderId)
                .collect(Collectors.toSet());

        // 批量获取用户信息并转换为Map映射
        List<FindUserByIdRspDTO> userInfoList = batchGetUserInfo(new ArrayList<>(userIds));
        Map<Long, FindUserByIdRspDTO> userInfoMap = userInfoList.stream()
                .collect(Collectors.toMap(FindUserByIdRspDTO::getId, user -> user, (existing, replacement) -> existing));

        // 构建响应
        return messageList.stream()
                .map(messagePO -> {
                    FindUserByIdRspDTO userInfo = userInfoMap.get(messagePO.getSenderId());

                    return RoomMessageResVO.builder()
                            .messageId(messagePO.getId())
                            .roomId(messagePO.getRoomId())
                            .senderId(messagePO.getSenderId())
                            .senderNickname(userInfo.getNickName())
                            .senderAvatar(userInfo.getAvatar())
                            .contentType(messagePO.getContentType())
                            .content(messagePO.getContent())
                            .messageType(messagePO.getMessageType())
                            .sendTime(messagePO.getCreatedTime())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 缓存消息到Redis
     */
    private void cacheMessageToRedis(RoomMessagePO messagePO, long currentTime) {
        try {
            String redisKey = String.format(RedisKeyConstants.ROOM_MESSAGE_KEY, messagePO.getRoomId());

            // 构建缓存对象
            RoomMessageResVO cacheVO = buildMessageResVO(messagePO);
            String jsonValue = JsonUtils.toJsonString(cacheVO);


            // 添加到ZSet,使用时间戳作为score
            redisTemplate.opsForZSet().add(redisKey, jsonValue, currentTime);

            // 保留最近100条消息
            Long size = redisTemplate.opsForZSet().size(redisKey);
            if (size != null && size > MAX_CACHE_MESSAGE_SIZE) {
                redisTemplate.opsForZSet().removeRange(redisKey, 0, size - MAX_CACHE_MESSAGE_SIZE - 1);
            }

            log.info("消息已缓存到Redis, messageId: {}, roomId: {}", messagePO.getId(), messagePO.getRoomId());
        } catch (Exception e) {
            log.error("消息缓存到Redis失败, messageId: {}", messagePO.getId(), e);
        }
    }

    /**
     * 发送消息到MQ
     */
    private void sendMessageToMQ(RoomMessagePO messagePO, long currentTime) {
        try {
            com.theoyu.thesis.media.api.dto.RoomMessageDTO messageDTO = com.theoyu.thesis.media.api.dto.RoomMessageDTO.builder()
                    .messageId(messagePO.getId())
                    .roomId(messagePO.getRoomId())
                    .senderId(messagePO.getSenderId())
                    .content(messagePO.getContent())
                    .messageType(messagePO.getMessageType())
                    .sendTime(currentTime)
                    .build();

            String topic = MQConstants.ROOM_MESSAGE_TOPIC;
            String tag = MQConstants.ROOM_MESSAGE_TAG;

            rocketMQTemplate.convertAndSend(topic + ":" + tag, messageDTO);
            log.info("消息已发送到MQ, messageId: {}, topic: {}", messagePO.getId(), topic);
        } catch (Exception e) {
            log.error("消息发送到MQ失败, messageId: {}", messagePO.getId(), e);
        }
    }

    /**
     * 构建消息响应VO
     */
    private RoomMessageResVO buildMessageResVO(RoomMessagePO messagePO) {
        FindUserByIdRspDTO userInfo = getUserInfo(messagePO.getSenderId());


        return RoomMessageResVO.builder()
                .messageId(messagePO.getId())
                .roomId(messagePO.getRoomId())
                .senderId(messagePO.getSenderId())
                .senderNickname(userInfo.getNickName())
                .contentType(messagePO.getMessageType())
                .senderAvatar(userInfo.getAvatar())
                .content(messagePO.getContent())
                .messageType(messagePO.getMessageType())
                .sendTime(messagePO.getCreatedTime())
                .build();
    }

    /**
     * 获取用户信息
     */
    private FindUserByIdRspDTO getUserInfo(Long userId) {
        try {
            return userRpcService.findById(userId);
        } catch (Exception e) {
            log.error("获取用户信息失败, userId: {}", userId, e);
            return null;
        }
    }

    /**
     * 批量获取用户信息
     */
    private List<FindUserByIdRspDTO> batchGetUserInfo(List<Long> userIds) {
        try {
            return userRpcService.findByIds(new ArrayList<>(userIds));
        } catch (Exception e) {
            log.error("批量获取用户信息失败", e);
            return Collections.emptyList();
        }
    }

}
