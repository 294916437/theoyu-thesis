package com.theoyu.thesis.media.biz.consumer;

import cn.hutool.json.JSONUtil;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 房间创建事件消费者
 *
 * 业务职责:
 * 1. 更新用户房间配额缓存
 * 2. 初始化房间统计数据
 * 3. 记录房间创建日志
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_ROOM_EVENT,
        selectorExpression = MQConstants.TAG_ROOM_CREATED,
        consumerGroup = "media-room-created-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3  // 最多重试3次
)
public class RoomCreatedConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Override
    public void onMessage(String message) {
        log.info("[RoomCreatedConsumer] Received message: {}", message);

        try {
            // 1. 解析消息
            Map<String, Object> msgMap = JSON.parseObject(message, Map.class);
            Long roomId = Long.valueOf(msgMap.get("roomId").toString());
            String roomNo = msgMap.get("roomNo").toString();
            Long hostId = Long.valueOf(msgMap.get("hostId").toString());
            String title = msgMap.get("title").toString();
            Long timestamp = Long.valueOf(msgMap.get("timestamp").toString());

            // 2. 幂等性检查(基于roomId,防止重复消费)
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT,
                    MQConstants.TAG_ROOM_CREATED,
                    roomId.toString()
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[RoomCreatedConsumer] Duplicate message detected - roomId: {}", roomId);
                return; // 已消费过,直接返回
            }

            // 3. 更新用户房间配额缓存(增加计数)
            updateUserRoomQuota(hostId);

            // 4. 初始化房间统计数据
            initRoomStats(roomId, roomNo, hostId, title, timestamp);


        } catch (Exception e) {
            // 抛出异常,触发RocketMQ重试机制
            throw new RuntimeException("Room created event consume failed", e);
        }
    }

    /**
     * 更新用户房间配额缓存
     */
    private void updateUserRoomQuota(Long userId) {
        try {
            String quotaKey = String.format(RedisKeyConstants.USER_ROOM_QUOTA_KEY, userId);

            // 方式1: 如果缓存存在,增加计数
            Long newCount = redisTemplate.opsForValue().increment(quotaKey, 1);

            // 设置过期时间(如果是新key)
            if (newCount != null && newCount == 1L) {
                redisTemplate.expire(quotaKey,
                        RedisKeyConstants.USER_ROOM_QUOTA_EXPIRE_TIME,
                        TimeUnit.SECONDS);
            }

            log.info("[RoomCreatedConsumer] Updated user room quota - userId: {}, count: {}",
                    userId, newCount);

        } catch (Exception e) {
            log.error("[RoomCreatedConsumer] Failed to update user room quota - userId: {}",
                    userId, e);
        }
    }

    /**
     * 初始化房间统计数据
     */
    private void initRoomStats(Long roomId, String roomNo, Long hostId,
                               String title, Long timestamp) {
        try {
            // 初始化房间统计Hash结构
            String statsKey = String.format("media:room:stats:%s", roomId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("roomId", roomId);
            stats.put("roomNo", roomNo);
            stats.put("hostId", hostId);
            stats.put("title", title);
            stats.put("createTime", timestamp);
            stats.put("totalParticipants", 0); // 总参与人数
            stats.put("peakParticipants", 0);  // 峰值人数
            stats.put("totalMessages", 0);     // 总消息数
            stats.put("duration", 0);          // 持续时长(秒)

            redisTemplate.opsForHash().putAll(statsKey, stats);
            // 统计数据保留7天
            redisTemplate.expire(statsKey, 7, TimeUnit.DAYS);

            log.info("[RoomCreatedConsumer] Initialized room stats - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[RoomCreatedConsumer] Failed to init room stats - roomId: {}",
                    roomId, e);
        }
    }

}