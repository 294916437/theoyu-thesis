package com.theoyu.thesis.media.biz.consumer;

import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomParticipantPOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 房间关闭事件消费者
 *
 * 业务职责:
 * 1. 清理房间相关缓存数据
 * 2. 更新所有参与者状态
 * 3. 统计房间数据(时长、人数等)
 * 4. 更新用户房间配额
 * 5. 归档房间数据
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_ROOM_EVENT,
        selectorExpression = MQConstants.TAG_ROOM_CLOSED,
        consumerGroup = "media-room-closed-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
public class RoomClosedConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource
    private RoomParticipantPOMapper roomParticipantPOMapper;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void onMessage(String message) {
        log.info("[RoomClosedConsumer] Received message: {}", message);

        try {
            // 1. 解析消息
            Map<String, Object> msgMap = JSON.parseObject(message, Map.class);
            Long roomId = Long.valueOf(msgMap.get("roomId").toString());
            Long hostId = Long.valueOf(msgMap.get("hostId").toString());
            Long timestamp = Long.valueOf(msgMap.get("timestamp").toString());

            // 2. 幂等性检查
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT,
                    MQConstants.TAG_ROOM_CLOSED,
                    roomId.toString()
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[RoomClosedConsumer] Duplicate message detected - roomId: {}", roomId);
                return;
            }

            // 3. 执行核心业务逻辑
            processRoomClosure(roomId, hostId, timestamp);


        } catch (Exception e) {
            throw new RuntimeException("Room closed event consume failed", e);
        }
    }

    /**
     * 处理房间关闭核心逻辑
     */
    private void processRoomClosure(Long roomId, Long hostId, Long timestamp) {
        // 1. 检查房间是否需要自动关闭
        String autoCloseKey = String.format(RedisKeyConstants.ROOM_AUTO_CLOSE_KEY, roomId);
        Boolean isAutoClose = redisTemplate.hasKey(autoCloseKey);

        if(Boolean.TRUE.equals(isAutoClose)) {
            // 验证房间确实为空
            String participantsKey = String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId);
            Long participantCount = redisTemplate.opsForSet().size(participantsKey);

            if (participantCount != null && participantCount > 0) {
                // 房间不为空，取消自动关闭
                redisTemplate.delete(autoCloseKey);
                log.info("[RoomClosedConsumer] Auto-close cancelled - room not empty, roomId: {}", roomId);
                return;
            }

            log.info("[RoomClosedConsumer] Auto-closing empty room - roomId: {}", roomId);
        }

        // 2. 更新所有在线参与者状态为"已离开"
        updateParticipantsStatus(roomId);
        // 3. 清理Redis缓存
        cleanupRoomCache(roomId);

        // 4. 更新用户房间配额(减少计数)
        updateUserRoomQuota(hostId);
    }

    /**
     * 更新所有参与者状态
     */
    private void updateParticipantsStatus(Long roomId) {
        try {

            LocalDateTime now = LocalDateTime.now();

            // 批量更新
            int updatedCount = roomParticipantPOMapper.batchUpdateStatusByRoomId(
                    roomId,
                    2,
                    1,
                    now
            );

            if(updatedCount == 0){
                log.warn("[RoomClosedConsumer] No active participants found for roomId: {}", roomId);
                return;
            }


        } catch (Exception e) {
            log.error("[RoomClosedConsumer] Failed to update participants status - roomId: {}",
                    roomId, e);
        }
    }


    /**
     * 清理房间相关缓存
     */
    private void cleanupRoomCache(Long roomId) {
        try {
            List<String> keysToDelete = Arrays.asList(
                    String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId),
                    String.format(RedisKeyConstants.ROOM_CONFIG_KEY, roomId),
                    String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId),
                    String.format("media:room:stats:%s", roomId)
            );

            Long deletedCount = redisTemplate.delete(keysToDelete);

            // 清理参与者详情缓存(使用scan查找所有相关key)
            String participantPattern = String.format("media:participant:%s:*", roomId);
            Set<String> participantKeys = redisTemplate.keys(participantPattern);
            if (!participantKeys.isEmpty()) {
                redisTemplate.delete(participantKeys);
            }

            log.info("[RoomClosedConsumer] Cleaned up room cache - roomId: {}, deleted: {}",
                    roomId, deletedCount);

        } catch (Exception e) {
            log.error("[RoomClosedConsumer] Failed to cleanup room cache - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 更新用户房间配额(减少计数)
     */
    private void updateUserRoomQuota(Long userId) {
        try {
            String quotaKey = String.format(RedisKeyConstants.USER_ROOM_QUOTA_KEY, userId);

            // 减少计数,但不允许为负数
            Long currentCount = redisTemplate.opsForValue().decrement(quotaKey, 1);

            if (currentCount != null && currentCount < 0) {
                redisTemplate.opsForValue().set(quotaKey, 0);
                currentCount = 0L;
            }

            log.info("[RoomClosedConsumer] Updated user room quota - userId: {}, count: {}",
                    userId, currentCount);

        } catch (Exception e) {
            log.error("[RoomClosedConsumer] Failed to update user room quota - userId: {}",
                    userId, e);
        }
    }

}