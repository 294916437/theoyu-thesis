package com.theoyu.thesis.media.biz.consumer;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.framework.common.utils.MapUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.model.dto.RoomCreatedEventDTO;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomParticipantPOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 房间创建事件消费者
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
    private RoomParticipantPOMapper roomParticipantPOMapper;


    @Override
    public void onMessage(String message) {
        log.info("[RoomCreatedConsumer] Received message: {}", message);

        try {
            // 1. 直接反序列化为 DTO 对象（类型安全）
            RoomCreatedEventDTO event = JsonUtils.parseObject(message, RoomCreatedEventDTO.class);

            // 2. 幂等性检查（基于 roomId）
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT,
                    MQConstants.TAG_ROOM_CREATED,
                    event.getRoomId().toString()
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[RoomCreatedConsumer] Duplicate message - roomId: {}", event.getRoomId());
                return;
            }

            // 3. 更新用户房间配额缓存
            updateUserRoomQuota(event.getHostId());

            // 4. 初始化房间统计数据
            initRoomStats(event);

            // 5. 新增房间参与者(创建者默认为主持人身份)
            LocalDateTime now = LocalDateTime.now();
            RoomParticipantPO roomParticipantPO = RoomParticipantPO.builder()
                    .roomId(event.getRoomId())
                    .userId(event.getHostId())
                    .role(2)   //主持人
                    .status(2) //离线
                    .videoMuted(true)
                    .audioMuted(true)
                    .createdTime(now)
                    .updatedTime(now)
                    .build();

            roomParticipantPOMapper.insert(roomParticipantPO);

        } catch (Exception e) {
            log.error("[RoomCreatedConsumer] Consume failed", e);
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
    private void initRoomStats(RoomCreatedEventDTO event) {
        try {
            String statsKey = String.format("media:room:stats:%s", event.getRoomId());

            Map<String, String> hashMap = MapUtils.objectToStringMap(event);

            // 存储到 Redis Hash
            redisTemplate.opsForHash().putAll(statsKey, hashMap);
            // 设置过期时间
            redisTemplate.expire(statsKey, RedisKeyConstants.MEDIA_STATS_EXPIRE_TIME,TimeUnit.SECONDS);

            log.info("[RoomCreatedConsumer] Initialized room stats - roomId: {}", event.getRoomId());

        } catch (Exception e) {
            log.error("[RoomCreatedConsumer] Failed to init room stats - roomId: {}",
                    event.getRoomId(), e);
        }
    }

}