package com.theoyu.thesis.media.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.model.dto.RecordingCompletedEventDTO;
import com.theoyu.thesis.media.biz.model.dto.RecordingStartedEventDTO;
import com.theoyu.thesis.media.biz.model.entity.RoomRecordPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomRecordPOMapper;
import com.theoyu.thesis.media.biz.model.vo.*;
import com.theoyu.thesis.media.biz.service.MediaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private RoomRecordPOMapper roomRecordPOMapper;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    // ==================== 开始录制 ====================

    /**
     * 开始录制（幂等）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartRecordingResVO startRecording(StartRecordingReqVO reqVO) {
        Long roomId = reqVO.getRoomId();
        Long userId = reqVO.getUserId();
        String statusKey = buildStatusKey(roomId, userId);

        // ---- Step 1: 查询 Redis 缓存 ----
        RoomRecordPO cached = getRecordFromCache(statusKey);
        if (cached != null) {
            if (cached.getStatus() != null && cached.getStatus() == 2) {
                // 已完成，直接返回已有结果
                log.info("[StartRecording] Redis 命中已完成录制: roomId={}, userId={}", roomId, userId);
                return buildExistsResVO(cached);
            }
            // status=0：缓存残留，清除后降级 DB 校验
            log.warn("[StartRecording] Redis 缓存为 RECORDING 状态，降级到 DB 校验: roomId={}, userId={}", roomId, userId);
            redisTemplate.delete(statusKey);
        }

        // ---- Step 2: 查询 DB，确保 roomId + hostId 始终只有一条记录 ----
        RoomRecordPO existing = roomRecordPOMapper.selectByRoomIdAndHostId(roomId, userId);
        if (existing != null) {
            // 已完成：幂等返回，回写缓存
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                log.info("[StartRecording] DB 命中已完成录制: roomId={}, userId={}", roomId, userId);
                putRecordToCache(statusKey, existing);
                return buildExistsResVO(existing);
            }

            // status=0（上次崩溃残留）：原地重置为新的 RECORDING 状态
            // 不新增记录，保证联合主键唯一性
            if (existing.getStatus() == 0) {
                log.warn("[StartRecording] 发现 RECORDING 残留记录，原地重置: roomId={}, userId={}", roomId, userId);
                LocalDateTime now = LocalDateTime.now();
                RoomRecordPO resetPO = RoomRecordPO.builder()
                        .roomId(roomId)
                        .hostId(userId)
                        .fileUrl(null)
                        .fileSize(null)
                        .duration(null)
                        .format(reqVO.getFormat())
                        .status((byte) 0)
                        .startTime(now)
                        .endTime(null)
                        .updatedAt(now)
                        .build();
                roomRecordPOMapper.updateByPrimaryKey(resetPO);
                putRecordToCache(statusKey, resetPO);
                asyncSendRecordingStartedEvent(roomId, userId, reqVO.getFormat(), now);
                return StartRecordingResVO.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .exists(false)
                        .format(reqVO.getFormat())
                        .startTime(now)
                        .build();
            }
        }

        // ---- Step 3: DB 无记录，新建录制记录 ----
        LocalDateTime now = LocalDateTime.now();
        RoomRecordPO newRecord = RoomRecordPO.builder()
                .roomId(roomId)
                .hostId(userId)
                .format(reqVO.getFormat())
                .status((byte) 0)   // 0: RECORDING
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        roomRecordPOMapper.insert(newRecord);
        log.info("[StartRecording] 创建新录制记录: roomId={}, userId={}", roomId, userId);

        putRecordToCache(statusKey, newRecord);

        // ---- Step 4: 异步 MQ（事务提交后发送）----
        asyncSendRecordingStartedEvent(roomId, userId, reqVO.getFormat(), now);

        return StartRecordingResVO.builder()
                .roomId(roomId)
                .userId(userId)
                .exists(false)
                .fileUrl(null)
                .format(reqVO.getFormat())
                .startTime(now)
                .build();
    }


    // ==================== 停止录制 ====================

    /**
     * 停止录制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StopRecordingResVO stopRecording(StopRecordingReqVO reqVO) {
        Long roomId = reqVO.getRoomId();
        Long userId = reqVO.getUserId();
        String statusKey = buildStatusKey(roomId, userId);

        // ---- Step 1: 校验录制记录是否存在且处于进行中状态 ----
        RoomRecordPO activeRecord = getActiveRecord(statusKey, roomId, userId);
        if (activeRecord == null) {
            log.warn("[StopRecording] 未找到进行中的录制记录: roomId={}, userId={}", roomId, userId);
            throw new BusinessException(ResponseCodeEnum.RECORDING_NOT_ACTIVE);
        }

        // ---- Step 2: 更新 DB ----
        LocalDateTime now = LocalDateTime.now();
        RoomRecordPO updatePO = RoomRecordPO.builder()
                .roomId(roomId)
                .hostId(userId)
                .fileUrl(reqVO.getFileUrl())
                .fileSize(reqVO.getFileSize())
                .duration(reqVO.getDuration())
                .format(reqVO.getFormat() != null ? reqVO.getFormat() : activeRecord.getFormat())
                .status((byte) 2)   // 2: COMPLETED
                .endTime(now)
                .updatedAt(now)
                .build();

        int updated = roomRecordPOMapper.updateCompletedInfo(updatePO);
        if (updated == 0) {
            log.error("[StopRecording] DB 更新失败: roomId={}, userId={}", roomId, userId);
            throw new BusinessException(ResponseCodeEnum.RECORDING_STOP_FAILED);
        }

        log.info("[StopRecording] 录制完成: roomId={}, userId={}, duration={}s, fileSize={}bytes",
                roomId, userId, reqVO.getDuration(), reqVO.getFileSize());

        // ---- Step 3: 删除 Redis 缓存（录制已结束）----
        redisTemplate.delete(statusKey);

        // ---- Step 4: 异步 MQ → 通知其他成员录制结束、触发归档等副作用（解耦）----
        asyncSendRecordingCompletedEvent(roomId, userId, reqVO, now);

        return StopRecordingResVO.builder()
                .fileUrl(reqVO.getFileUrl())
                .fileSize(reqVO.getFileSize())
                .duration(reqVO.getDuration())
                .endTime(now)
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 Redis Key：room:recording:{roomId}:{hostId}
     */
    private String buildStatusKey(Long roomId, Long userId) {
        return String.format(RedisKeyConstants.ROOM_RECORDING_STATUS_KEY, roomId, userId);
    }

    /**
     * 从 Redis Hash 中读取录制记录
     * 一次 hGetAll 获取所有字段，减少网络 RTT
     */
    private RoomRecordPO getRecordFromCache(String statusKey) {
        try {
            Object raw = redisTemplate.opsForValue().get(statusKey);
            if (raw == null) return null;
            return JsonUtils.parseObject(raw.toString(), RoomRecordPO.class);
        } catch (Exception e) {
            log.warn("[Recording] Redis 缓存读取失败，降级查 DB: key={}, err={}", statusKey, e.getMessage());
            return null;
        }
    }

    /**
     * 将录制记录序列化后写入 Redis String
     * TTL = 7 天（录制完成后用户仍可查看历史结果）
     */
    private void putRecordToCache(String statusKey, RoomRecordPO record) {
        try {
            redisTemplate.opsForValue().set(
                    statusKey,
                    JsonUtils.toJsonString(record),
                    RedisKeyConstants.ROOM_RECORDING_STATUS_EXPIRE_TIME,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            // 缓存写入失败不影响主流程，仅打印警告
            log.warn("[Recording] Redis 缓存写入失败: key={}, err={}", statusKey, e.getMessage());
        }
    }

    /**
     * 获取进行中（status=0）的录制记录
     * 优先 Redis，未命中降级查 DB
     * DB 命中时仅当 status=0 才视为有效（防止重复停止）
     */
    private RoomRecordPO getActiveRecord(String statusKey, Long roomId, Long userId) {
        // 先查 Redis
        RoomRecordPO cached = getRecordFromCache(statusKey);
        if (cached != null) {
            if (cached.getStatus() != null && cached.getStatus() == 0) {
                return cached;
            }
            // 缓存中已是完成状态，说明已停止过
            log.warn("[StopRecording] Redis 缓存显示录制已完成，拒绝重复停止: roomId={}, userId={}", roomId, userId);
            return null;
        }

        // 降级查 DB
        RoomRecordPO dbRecord = roomRecordPOMapper.selectByRoomIdAndHostId(roomId, userId);
        if (dbRecord == null || dbRecord.getStatus() == null || dbRecord.getStatus() != 0) {
            return null;
        }
        return dbRecord;
    }

    /**
     * 将已有录制记录转换为 StartRecordingResVO（exists=true）
     */
    private StartRecordingResVO buildExistsResVO(RoomRecordPO record) {
        return StartRecordingResVO.builder()
                .roomId(record.getRoomId())
                .userId(record.getHostId())
                .exists(true)
                .fileUrl(record.getFileUrl())
                .fileSize(record.getFileSize())
                .duration(record.getDuration())
                .format(record.getFormat())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .build();
    }

    /**
     * 异步发送「录制开始」MQ 消息
     * 用于：WebSocket 通知房间内其他成员"录制已开始"（副作用，不影响主流程）
     */
    private void asyncSendRecordingStartedEvent(Long roomId, Long userId, String format, LocalDateTime startTime) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                threadPoolTaskExecutor.execute(() -> {
                    try {
                        RecordingStartedEventDTO eventDTO = RecordingStartedEventDTO.builder()
                                .roomId(roomId)
                                .hostId(userId)
                                .format(format)
                                .startTime(startTime)
                                .build();
                        rocketMQTemplate.syncSend(
                                MQConstants.ROOM_RECORD_TOPIC + ":" + MQConstants.TAG_RECORDING_STARTED,
                                MessageBuilder.withPayload(JsonUtils.toJsonString(eventDTO)).build()
                        );
                    } catch (Exception e) {
                        log.error("[MQ] 发送录制开始事件失败: roomId={}, userId={}", roomId, userId, e);
                    }
                });
            }
        });
    }

    /**
     * 异步发送「录制完成」MQ 消息
     * 用于：WebSocket 通知房间内成员、触发 VOD 归档等后续副作用（解耦）
     */
    private void asyncSendRecordingCompletedEvent(Long roomId, Long userId,
                                                  StopRecordingReqVO reqVO, LocalDateTime endTime) {
        // 注册事务提交后回调，确保 DB 提交成功后才发送 MQ
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                threadPoolTaskExecutor.execute(() -> {
                    try {
                        RecordingCompletedEventDTO eventDTO = RecordingCompletedEventDTO.builder()
                                .roomId(roomId)
                                .hostId(userId)
                                .fileUrl(reqVO.getFileUrl())
                                .fileSize(reqVO.getFileSize())
                                .duration(reqVO.getDuration())
                                .endTime(endTime)
                                .build();
                        rocketMQTemplate.syncSend(
                                MQConstants.ROOM_RECORD_TOPIC + ":" + MQConstants.TAG_RECORDING_COMPLETED,
                                MessageBuilder.withPayload(JsonUtils.toJsonString(eventDTO)).build()
                        );
                        log.debug("[MQ] 发送录制完成事件成功: roomId={}, userId={}", roomId, userId);
                    } catch (Exception e) {
                        log.error("[MQ] 发送录制完成事件失败: roomId={}, userId={}", roomId, userId, e);
                    }
                });
            }
        });
    }
}