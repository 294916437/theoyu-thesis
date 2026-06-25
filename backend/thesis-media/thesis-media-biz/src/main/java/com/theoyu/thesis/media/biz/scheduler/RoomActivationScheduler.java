package com.theoyu.thesis.media.biz.scheduler;

import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.service.RoomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 预约会议自动激活调度器。
 * 每隔30秒轮询 Redis ZSet（room:activation:queue），
 * 将 score（激活时刻 epoch 秒）小于等于当前时间的会议状态由 0（预约中）更新为 1（进行中）。
 * 使用 ZSet 作为延迟队列，重启后任务不丢失。
 */
@Component
@Slf4j
public class RoomActivationScheduler {

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoomService roomService;

    @Scheduled(fixedDelay = 30_000)
    public void activateDueRooms() {
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                RedisKeyConstants.ROOM_ACTIVATION_SCHEDULER_LOCK_KEY,
                lockValue,
                25,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            double nowEpoch = System.currentTimeMillis() / 1000.0;
            // 取出所有 score <= 当前时间的条目
            Set<Object> dueRoomIds = redisTemplate.opsForZSet()
                    .rangeByScore(RedisKeyConstants.ROOM_ACTIVATION_QUEUE_KEY, 0, nowEpoch);

            if (dueRoomIds == null || dueRoomIds.isEmpty()) {
                return;
            }

            log.info("[RoomActivationScheduler] 检测到 {} 个待激活会议", dueRoomIds.size());

            for (Object obj : dueRoomIds) {
                Long roomId = Long.parseLong(obj.toString());
                try {
                    roomService.activateRoom(roomId);
                    redisTemplate.opsForZSet()
                            .remove(RedisKeyConstants.ROOM_ACTIVATION_QUEUE_KEY, obj);
                } catch (Exception e) {
                    log.error("[RoomActivationScheduler] 激活会议失败 - roomId: {}", roomId, e);
                }
            }
        } catch (Exception e) {
            log.error("[RoomActivationScheduler] 轮询激活队列异常", e);
        } finally {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(RedisKeyConstants.ROOM_ACTIVATION_SCHEDULER_LOCK_KEY),
                    lockValue);
        }
    }
}
