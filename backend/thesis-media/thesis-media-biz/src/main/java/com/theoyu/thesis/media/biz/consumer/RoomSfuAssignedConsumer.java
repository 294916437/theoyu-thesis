package com.theoyu.thesis.media.biz.consumer;

import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.model.dto.RoomSfuAssignedEventDTO;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 房间 SFU 节点分配事件消费者
 *
 * 业务职责:
 * 1. 更新 Redis 中的房间-SFU 节点绑定缓存
 * 2. 缓存 SFU 服务器地址供前端使用
 * 3. 验证消息幂等性，防止重复处理
 * 4. 记录 SFU 分配事件日志
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_ROOM_EVENT,
        selectorExpression = MQConstants.TAG_ROOM_SFU_ASSIGNED,
        consumerGroup = "media-room-sfu-assigned-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3  // 最多重试 3 次
)
public class RoomSfuAssignedConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Override
    public void onMessage(String message) {
        log.info("[RoomSfuAssignedConsumer] Received message: {}", message);

        try {
            // 1. 反序列化消息为 DTO 对象
            RoomSfuAssignedEventDTO event = JsonUtils.parseObject(message, RoomSfuAssignedEventDTO.class);

            if (event == null || event.getRoomId() == null || event.getSfuNodeId() == null) {
                log.error("[RoomSfuAssignedConsumer] Invalid event: {}", message);
                throw new IllegalArgumentException("Invalid RoomSfuAssignedEventDTO");
            }

            // 2. 幂等性检查（基于 roomId）
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_ROOM_EVENT,
                    MQConstants.TAG_ROOM_SFU_ASSIGNED,
                    event.getRoomId().toString()
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.warn("[RoomSfuAssignedConsumer] Duplicate message - roomId: {}, sfuNodeId: {}",
                        event.getRoomId(), event.getSfuNodeId());
                return;
            }

            // 3. 缓存房间-SFU 节点绑定关系
            cacheRoomSfuBinding(event.getRoomId(), event.getSfuNodeId());

            // 4. 缓存 SFU 服务器地址（供前端直连使用）
            cacheSfuServerUrl(event.getRoomId(), event.getSfuServerUrl(), event.getSfuNodeId());

            // 5. 异步更新房间状态（确保 DB 与 Redis 一致）
            updateRoomSfuInfoInDB(event);

            log.info("[RoomSfuAssignedConsumer] Room SFU assignment processed successfully - " +
                    "roomId: {}, sfuNodeId: {}, instanceId: {}, assignedBy: {}",
                    event.getRoomId(), event.getSfuNodeId(), event.getInstanceId(), event.getAssignedByUserId());

        } catch (Exception e) {
            log.error("[RoomSfuAssignedConsumer] Message processing failed", e);
            throw new RuntimeException("Room SFU assigned event consume failed", e);
        }
    }

    /**
     * 缓存房间-SFU 节点绑定关系
     * 
     * @param roomId 房间ID
     * @param sfuNodeId SFU 节点ID
     */
    private void cacheRoomSfuBinding(Long roomId, Long sfuNodeId) {
        try {
            String key = String.format(RedisKeyConstants.ROOM_SFU_NODE_KEY, roomId);
            redisTemplate.opsForValue().set(
                    key,
                    sfuNodeId,
                    RedisKeyConstants.ROOM_SFU_NODE_EXPIRE_TIME,
                    TimeUnit.SECONDS
            );
            log.debug("[RoomSfuAssignedConsumer] Room-SFU binding cached - roomId: {}, sfuNodeId: {}",
                    roomId, sfuNodeId);
        } catch (Exception e) {
            log.warn("[RoomSfuAssignedConsumer] Failed to cache room-SFU binding - roomId: {}", roomId, e);
            // 缓存失败不影响主流程
        }
    }

    /**
     * 缓存 SFU 服务器地址
     * 
     * 前端使用返回的 sfuServerUrl 通过 socket.io 直接连接到 SFU 节点
     * 缓存可加速后续热路径查询
     * 
     * @param roomId 房间ID
     * @param sfuServerUrl SFU 服务器地址（wss://host:port 或 ws://host:port）
     * @param sfuNodeId SFU 节点ID
     */
    private void cacheSfuServerUrl(Long roomId, String sfuServerUrl, Long sfuNodeId) {
        try {
            if (sfuServerUrl != null && !sfuServerUrl.isBlank()) {
                String key = String.format(RedisKeyConstants.ROOM_SFU_URL_KEY, roomId);
                redisTemplate.opsForValue().set(
                        key,
                        sfuServerUrl,
                        RedisKeyConstants.ROOM_SFU_NODE_EXPIRE_TIME,
                        TimeUnit.SECONDS
                );
                log.debug("[RoomSfuAssignedConsumer] SFU server URL cached - roomId: {}, url: {}",
                        roomId, sfuServerUrl);
            }
        } catch (Exception e) {
            log.warn("[RoomSfuAssignedConsumer] Failed to cache SFU server URL - roomId: {}", roomId, e);
            // 缓存失败不影响主流程
        }
    }

    /**
     * 同步更新房间信息中的 SFU 节点 ID（确保 DB 与 Redis 一致）
     * 
     * @param event SFU 分配事件 DTO
     */
    private void updateRoomSfuInfoInDB(RoomSfuAssignedEventDTO event) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, event.getRoomId());
            // 同步更新房间缓存中的 sfuNodeId
            redisTemplate.opsForHash().put(roomKey, "sfuNodeId", String.valueOf(event.getSfuNodeId()));
            // 更新 updatedTime
            long currentTimestamp = System.currentTimeMillis();
            redisTemplate.opsForHash().put(roomKey, "updatedTime", String.valueOf(currentTimestamp));

            // 刷新房间缓存过期时间
            redisTemplate.expire(roomKey, RedisKeyConstants.ROOM_INFO_EXPIRE_TIME, TimeUnit.SECONDS);

            log.debug("[RoomSfuAssignedConsumer] Room SFU info updated in cache - roomId: {}, sfuNodeId: {}",
                    event.getRoomId(), event.getSfuNodeId());
        } catch (Exception e) {
            log.warn("[RoomSfuAssignedConsumer] Failed to update room SFU info in cache - roomId: {}",
                    event.getRoomId(), e);
            // 缓存更新失败不影响主流程
        }
    }
}
