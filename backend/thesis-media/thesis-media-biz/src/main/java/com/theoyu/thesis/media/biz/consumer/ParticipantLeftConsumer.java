package com.theoyu.thesis.media.biz.consumer;

import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 参与者离开事件消费者
 *
 * 业务职责:
 * 1. 更新房间统计数据（当前在线人数）
 * 2. 更新房间在线人数缓存
 * 3. 检查房间是否为空，触发自动关闭
 * 4. 清理用户相关临时数据
 * 5. 记录用户行为日志
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_ROOM_EVENT,
        selectorExpression = MQConstants.TAG_PARTICIPANT_LEFT,
        consumerGroup = "media-participant-left-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
public class ParticipantLeftConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 空房间自动关闭延迟时间（秒）
     * 避免用户短暂离开后立即关闭房间
     */
    private static final long EMPTY_ROOM_AUTO_CLOSE_DELAY = 300; // 5分钟

    @Override
    public void onMessage(String message) {
        log.info("[ParticipantLeftConsumer] Received message: {}", message);

        try {
            // 1. 解析消息
            Map<String, Object> msgMap = JSON.parseObject(message, Map.class);
            String roomId = msgMap.get("roomId").toString();
            String userId = msgMap.get("userId").toString();
            String username = msgMap.get("username").toString();
            Long timestamp = Long.valueOf(msgMap.get("timestamp").toString());

            // 2. 幂等性检查
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT,
                    MQConstants.TAG_PARTICIPANT_LEFT,
                    roomId + ":" + userId + ":" + timestamp
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[ParticipantLeftConsumer] Duplicate message - roomId: {}, userId: {}",
                        roomId, userId);
                return;
            }

            // 3. 执行核心业务逻辑
            processParticipantLeft(roomId, userId, username, timestamp);

            log.info("[ParticipantLeftConsumer] Process success - roomId: {}, userId: {}",
                    roomId, userId);

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Process failed", e);
            throw new RuntimeException("Participant left event consume failed", e);
        }
    }

    /**
     * 处理参与者离开核心逻辑
     */
    private void processParticipantLeft(String roomId, String userId,
                                        String username, Long timestamp) {
        // 1. 更新房间统计数据
        updateRoomStats(roomId, false);

        // 2. 更新在线人数缓存
        Long currentOnline = updateOnlineCount(roomId, false);

        // 3. 检查是否需要自动关闭房间
        if (currentOnline != null && currentOnline == 0) {
            scheduleAutoCloseRoom(roomId);
        }

        // 4. 清理用户临时数据（异步）
        taskExecutor.execute(() -> cleanupUserTempData(roomId, userId));

        // 5. 记录行为日志（异步）
        taskExecutor.execute(() -> logUserBehavior(roomId, userId, username, "left", timestamp));
    }

    /**
     * 更新房间统计数据
     */
    private void updateRoomStats(String roomId, boolean isJoin) {
        try {
            String statsKey = String.format("media:room:stats:%s", roomId);

            if (!isJoin) {
                // 当前在线人数 -1（但不能小于0）
                Long currentOnline = redisTemplate.opsForHash()
                        .increment(statsKey, "currentParticipants", -1);

                if (currentOnline < 0) {
                    redisTemplate.opsForHash().put(statsKey, "currentParticipants", 0);
                }
            }

            // 更新最后活跃时间
            redisTemplate.opsForHash().put(statsKey, "lastActiveTime", System.currentTimeMillis());

            log.debug("[ParticipantLeftConsumer] Room stats updated - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to update room stats - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 更新在线人数缓存
     * @return 当前在线人数
     */
    private Long updateOnlineCount(String roomId, boolean isJoin) {
        try {
            String participantsKey = String.format(RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);

            // 获取当前在线人数
            Long onlineCount = redisTemplate.opsForSet().size(participantsKey);

            if (onlineCount == null) {
                onlineCount = 0L;
            }

            // 更新房间信息缓存中的在线人数字段
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);
            redisTemplate.opsForHash().put(roomKey, "currentParticipants", onlineCount.toString());

            log.debug("[ParticipantLeftConsumer] Online count updated - roomId: {}, count: {}",
                    roomId, onlineCount);

            return onlineCount;

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to update online count - roomId: {}",
                    roomId, e);
            return null;
        }
    }

    /**
     * 调度空房间自动关闭
     * 使用Redis过期Key + KeySpace通知实现延迟关闭
     */
    private void scheduleAutoCloseRoom(String roomId) {
        try {
            // 检查房间是否设置了自动关闭标记
            String autoCloseKey = String.format("media:room:auto-close:%s", roomId);

            // 使用SETNX确保只有一个线程设置自动关闭
            Boolean setSuccess = redisTemplate.opsForValue()
                    .setIfAbsent(autoCloseKey, "1", EMPTY_ROOM_AUTO_CLOSE_DELAY, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(setSuccess)) {
                log.info("[ParticipantLeftConsumer] Room scheduled for auto-close - roomId: {}, " +
                        "delay: {} seconds", roomId, EMPTY_ROOM_AUTO_CLOSE_DELAY);

                // 使用延迟队列或定时任务检查房间状态
                // 方案1: 使用RocketMQ延迟消息
                scheduleAutoCloseViaMQ(roomId);

                // 方案2: 使用Redis KeySpace通知（需要开启通知功能）
                // 方案3: 使用定时任务扫描（推荐用于生产环境）
            }

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to schedule auto-close - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 通过RocketMQ延迟消息实现自动关闭
     */
    private void scheduleAutoCloseViaMQ(String roomId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("roomId", roomId);
            message.put("timestamp", System.currentTimeMillis());
            message.put("reason", "empty_room_auto_close");

            // RocketMQ延迟级别: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
            // 5分钟对应延迟级别 10 (6m，稍长一点避免误关闭)
            org.springframework.messaging.Message<String> mqMessage =
                    org.springframework.messaging.support.MessageBuilder
                            .withPayload(JSON.toJSONString(message))
                            .build();

            rocketMQTemplate.syncSend(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_ROOM_CLOSED,
                    mqMessage,
                    3000, // 超时时间
                    10    // 延迟级别：6分钟
            );

            log.info("[ParticipantLeftConsumer] Auto-close message sent - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to send auto-close message - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 清理用户临时数据
     */
    private void cleanupUserTempData(String roomId, String userId) {
        try {
            // 清理用户在该房间的临时状态数据
            String userStateKey = String.format("media:user:state:%s:%s", roomId, userId);
            redisTemplate.delete(userStateKey);

            // 清理用户媒体统计数据（保留一段时间用于分析）
            String userStatsPattern = String.format("media:stats:%s:%s:*", roomId, userId);
            // 注意：这里不立即删除，由过期时间自动清理

            log.debug("[ParticipantLeftConsumer] User temp data cleaned - roomId: {}, userId: {}",
                    roomId, userId);

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to cleanup user temp data - " +
                    "roomId: {}, userId: {}", roomId, userId, e);
        }
    }

    /**
     * 记录用户行为日志
     */
    private void logUserBehavior(String roomId, String userId, String username,
                                 String action, Long timestamp) {
        try {
            String behaviorKey = String.format("media:user:behavior:%s", userId);

            String behaviorData = JSON.toJSONString(Map.of(
                    "roomId", roomId,
                    "username", username,
                    "action", action,
                    "timestamp", timestamp
            ));

            redisTemplate.opsForZSet().add(behaviorKey, behaviorData, timestamp.doubleValue());

            // 只保留最近30天的记录
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            redisTemplate.opsForZSet().removeRangeByScore(behaviorKey, 0, thirtyDaysAgo);

            redisTemplate.expire(behaviorKey, 35, TimeUnit.DAYS);

            log.debug("[ParticipantLeftConsumer] User behavior logged - userId: {}, action: {}",
                    userId, action);

        } catch (Exception e) {
            log.error("[ParticipantLeftConsumer] Failed to log user behavior - userId: {}",
                    userId, e);
        }
    }
}