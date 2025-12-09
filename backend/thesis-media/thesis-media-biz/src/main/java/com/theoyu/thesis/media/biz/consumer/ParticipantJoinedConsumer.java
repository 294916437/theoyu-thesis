package com.theoyu.thesis.media.biz.consumer;

import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 参与者加入事件消费者
 *
 * 业务职责:
 * 1. 更新房间统计数据（总参与人数、峰值人数、当前在线人数）
 * 2. 更新房间在线人数缓存
 * 3. 记录用户行为日志
 * 4. 检查是否需要扩容（参与者接近上限时预警）
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_ROOM_EVENT,
        selectorExpression = MQConstants.TAG_PARTICIPANT_JOINED,
        consumerGroup = "media-participant-joined-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
public class ParticipantJoinedConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(String message) {
        log.info("[ParticipantJoinedConsumer] Received message: {}", message);

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
                    MQConstants.TAG_PARTICIPANT_JOINED,
                    roomId + ":" + userId + ":" + timestamp
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[ParticipantJoinedConsumer] Duplicate message - roomId: {}, userId: {}",
                        roomId, userId);
                return;
            }

            // 3. 执行核心业务逻辑
            processParticipantJoined(roomId, userId, username, timestamp);

            log.info("[ParticipantJoinedConsumer] Process success - roomId: {}, userId: {}",
                    roomId, userId);

        } catch (Exception e) {
            log.error("[ParticipantJoinedConsumer] Process failed", e);
            throw new RuntimeException("Participant joined event consume failed", e);
        }
    }

    /**
     * 处理参与者加入核心逻辑
     */
    private void processParticipantJoined(String roomId, String userId,
                                          String username, Long timestamp) {
        // 1. 更新房间统计数据
        updateRoomStats(roomId, true);

        // 2. 更新在线人数缓存
        updateOnlineCount(roomId, true);

        // 3. 检查房间容量预警（异步）
        taskExecutor.execute(() -> checkRoomCapacityWarning(roomId));

        // 4. 记录行为日志（异步）
        taskExecutor.execute(() -> logUserBehavior(roomId, userId, username, "joined", timestamp));
    }

    /**
     * 更新房间统计数据
     */
    private void updateRoomStats(String roomId, boolean isJoin) {
        try {
            String statsKey = String.format("media:room:stats:%s", roomId);

            // 使用Hash自增操作，原子性更新
            if (isJoin) {
                // 总参与人数 +1
                redisTemplate.opsForHash().increment(statsKey, "totalParticipants", 1);

                // 当前在线人数 +1
                Long currentOnline = redisTemplate.opsForHash()
                        .increment(statsKey, "currentParticipants", 1);

                // 更新峰值人数
                Object peakObj = redisTemplate.opsForHash().get(statsKey, "peakParticipants");
                long peak = peakObj != null ? Long.parseLong(peakObj.toString()) : 0L;

                if (currentOnline > peak) {
                    redisTemplate.opsForHash().put(statsKey, "peakParticipants", currentOnline);
                    log.info("[ParticipantJoinedConsumer] New peak participants - roomId: {}, peak: {}",
                            roomId, currentOnline);
                }
            }

            // 更新最后活跃时间
            redisTemplate.opsForHash().put(statsKey, "lastActiveTime", System.currentTimeMillis());

            log.debug("[ParticipantJoinedConsumer] Room stats updated - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[ParticipantJoinedConsumer] Failed to update room stats - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 更新在线人数缓存
     */
    private void updateOnlineCount(String roomId, boolean isJoin) {
        try {
            String participantsKey = String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId);

            // 获取当前在线人数（从Set中获取）
            Long onlineCount = redisTemplate.opsForSet().size(participantsKey);

            if (onlineCount == null) {
                onlineCount = 0L;
            }

            // 更新房间信息缓存中的在线人数字段
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);
            redisTemplate.opsForHash().put(roomKey, "currentParticipants", onlineCount.toString());

            log.debug("[ParticipantJoinedConsumer] Online count updated - roomId: {}, count: {}",
                    roomId, onlineCount);

        } catch (Exception e) {
            log.error("[ParticipantJoinedConsumer] Failed to update online count - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 检查房间容量预警
     */
    private void checkRoomCapacityWarning(String roomId) {
        try {
            String participantsKey = String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId);
            Long currentCount = redisTemplate.opsForSet().size(participantsKey);

            if (currentCount == null) {
                return;
            }

            // 从缓存或数据库获取房间最大人数
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);
            Object maxParticipantsObj = redisTemplate.opsForHash().get(roomKey, "maxParticipants");

            Integer maxParticipants;
            if (maxParticipantsObj != null) {
                maxParticipants = Integer.valueOf(maxParticipantsObj.toString());
            } else {
                // 缓存未命中，查询数据库
                RoomPO room = roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));
                maxParticipants = room != null ? room.getMaxParticipants() : 100;
            }

            // 如果达到80%容量，记录预警日志
            double usageRate = (double) currentCount / maxParticipants;
            if (usageRate >= 0.8) {
                log.warn("[ParticipantJoinedConsumer] Room capacity warning - roomId: {}, " +
                                "current: {}, max: {}, usage: {}%",
                        roomId, currentCount, maxParticipants, String.format("%.1f", usageRate * 100));

                // 可以在这里发送告警通知（邮件、短信、钉钉等）
                // alertService.sendCapacityWarning(roomId, currentCount, maxParticipants);
            }

        } catch (Exception e) {
            log.error("[ParticipantJoinedConsumer] Failed to check room capacity - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 记录用户行为日志
     */
    private void logUserBehavior(String roomId, String userId, String username,
                                 String action, Long timestamp) {
        try {
            // 使用Redis Sorted Set记录用户行为时间线
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

            // 设置过期时间（35天，留一些buffer）
            redisTemplate.expire(behaviorKey, 35, TimeUnit.DAYS);

            log.debug("[ParticipantJoinedConsumer] User behavior logged - userId: {}, action: {}",
                    userId, action);

        } catch (Exception e) {
            log.error("[ParticipantJoinedConsumer] Failed to log user behavior - userId: {}",
                    userId, e);
        }
    }
}