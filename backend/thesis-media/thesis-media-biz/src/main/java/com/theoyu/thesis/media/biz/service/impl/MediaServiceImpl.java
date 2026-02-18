package com.theoyu.thesis.media.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.utils.MapUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.grpc.SFUGrpcClient;
import com.theoyu.thesis.media.biz.grpc.proto.control.*;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private SFUGrpcClient sfuGrpcClient;
    @Resource
    private RoomRecordPOMapper roomRecordPOMapper;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 启动录制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartRecordingResVO startRecording(StartRecordingReqVO reqVO) {
        Long roomId = reqVO.getRoomId();
        Long hostId = reqVO.getHostId();
        String activeKey = String.format(RedisKeyConstants.ROOM_RECORDING_ACTIVE_KEY, roomId);

        // 1. 快速检查：Redis 互斥锁，防止重复启动
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(
                activeKey,
                hostId.toString(), // 存 String 类型的 ID
                RedisKeyConstants.ROOM_RECORDING_ACTIVE_EXPIRE_TIME,
                TimeUnit.SECONDS
        );

        if (Boolean.FALSE.equals(isLocked)) {
            // 如果锁存在，进一步确认
            throw new BusinessException(ResponseCodeEnum.RECORDING_ALREADY_STARTED);
        }

        // 2. MySQL 兜底检查（防止 Redis 状态丢失导致的一致性问题）
        RoomRecordPO activeRecord = roomRecordPOMapper.selectActiveByRoomId(roomId);
        if (activeRecord != null) {
            log.warn("[StartRecording] 数据库存在进行中的录制记录: {}", roomId);
            throw new BusinessException(ResponseCodeEnum.RECORDING_ALREADY_STARTED);
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. 组装 gRPC 的录制配置参数
        // TODO:根据quality动态调整参数
        RecordingConfig config = RecordingConfig.newBuilder()
                .setVideoWidth(1280)
                .setVideoHeight(720)
                .setFormat(reqVO.getFormat())
                .build();

        StartRecordingResponse grpcResponse;
        try {
            // 通过 grpc 调用SFU端的开始会议录制的核心逻辑
            grpcResponse = sfuGrpcClient.startRecording(String.valueOf(roomId), String.valueOf(hostId), config);
            if (!grpcResponse.getSuccess()) {
                // 如果 grpc 明确返回失败，释放锁
                redisTemplate.delete(activeKey);
                log.error("[StartRecording] SFU 返回失败: {}", grpcResponse.getMessage());
                throw new BusinessException(ResponseCodeEnum.RECORDING_START_FAILED);
            }
        } catch (Exception e) {
            // 网络异常等
            redisTemplate.delete(activeKey);
            log.error("[StartRecording] 调用 gRPC 异常", e);
            throw new BusinessException(ResponseCodeEnum.SFU_NODE_UNAVAILABLE);
        }

        // 4. 持久化 DB (Status: 0-录制中)
        RoomRecordPO recordPO = RoomRecordPO.builder()
                .roomId(roomId)
                .hostId(hostId)
                .format(reqVO.getFormat())
                .status((byte) 0) // 0: RECORDING
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .duration(0)
                .fileSize(0)
                .build();

        roomRecordPOMapper.insert(recordPO);

        // 5. 写入 Redis Hash 缓存录制状态
        String statusKey = String.format(RedisKeyConstants.ROOM_RECORDING_STATUS_KEY, roomId);
        Map<String, String> cacheMap = MapUtils.objectToStringMap(recordPO);
        redisTemplate.opsForHash().putAll(statusKey, cacheMap);
        redisTemplate.expire(statusKey, RedisKeyConstants.ROOM_RECORDING_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);

        // 6. 异步发送 RocketMQ 消息
        threadPoolTaskExecutor.execute(() -> {
            try {
                RecordingStartedEventDTO eventDTO = RecordingStartedEventDTO.builder()
                        .roomId(roomId)
                        .hostId(hostId)
                        .format(reqVO.getFormat())
                        .startTime(now)
                        .timestamp(LocalDateTime.now())
                        .build();

                rocketMQTemplate.syncSend(
                        MQConstants.ROOM_RECORD_TOPIC + ":" + MQConstants.TAG_RECORDING_STARTED,
                        MessageBuilder.withPayload(eventDTO).build()
                );
            } catch (Exception e) {
                log.error("[RocketMQ] 发送录制开始事件失败", e);
            }
        });

        return StartRecordingResVO.builder()
                .roomId(roomId)
                .hostId(hostId)
                .status(0)
                .format(reqVO.getFormat())
                .startTime(now)
                .message("录制已启动")
                .build();
    }

    /**
     * 停止录制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StopRecordingResVO stopRecording(StopRecordingReqVO reqVO) {
        Long roomId = reqVO.getRoomId();
        Long hostId = reqVO.getHostId();

        // 1. 校验是否在录制中（先查询 Redis 锁）
        String activeKey = String.format(RedisKeyConstants.ROOM_RECORDING_ACTIVE_KEY, roomId);
        String statusKey = String.format(RedisKeyConstants.ROOM_RECORDING_STATUS_KEY, roomId);

        if (Boolean.FALSE.equals(redisTemplate.hasKey(activeKey))) {
            log.warn("[StopRecording] 房间未处于活跃录制状态: {}", roomId);
            throw new BusinessException(ResponseCodeEnum.RECORDING_NOT_ACTIVE);
        }

        // 2. 优化：在停止前先查询一次当前状态，以获取 duration 和 fileSize
        // 因为 StopRecordingResponse (proto) 中不包含这些统计信息，只包含 file_url
        long currentDuration = 0;
        long currentFileSize = 0;
        try {
            // 通过 grpc 调用SFU端的停止会议录制的核心逻辑
            RecordingStatusResponse statusRes = sfuGrpcClient.getRecordingStatus(String.valueOf(roomId), String.valueOf(hostId));
            if (statusRes != null) {
                currentDuration = statusRes.getDurationSeconds();
                currentFileSize = statusRes.getFileSizeBytes();
            }
        } catch (Exception e) {
            log.warn("[StopRecording] 获取最终录制状态统计失败，将使用默认值", e);
        }

        // 3. 调用 gRPC 停止录制
        StopRecordingResponse grpcResponse;
        try {
            //
            grpcResponse = sfuGrpcClient.stopRecording(String.valueOf(roomId), String.valueOf(hostId));
            if (!grpcResponse.getSuccess()) {
                throw new BusinessException(ResponseCodeEnum.RECORDING_STOP_FAILED);
            }
        } catch (Exception e) {
            log.error("[StopRecording] 调用 gRPC 异常", e);
            throw new BusinessException(ResponseCodeEnum.SFU_NODE_UNAVAILABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        // Double check DB 中的开始时间来计算时长，作为备选方案（如果 gRPC status 失败）
        if (currentDuration == 0) {
            RoomRecordPO activeRecord = roomRecordPOMapper.selectActiveByRoomId(roomId);
            if (activeRecord != null && activeRecord.getStartTime() != null) {
                currentDuration = Duration.between(activeRecord.getStartTime(), now).getSeconds();
            }
        }

        // 4. 更新 DB
        RoomRecordPO updatePO = RoomRecordPO.builder()
                .roomId(roomId)
                .hostId(hostId)
                .fileUrl(grpcResponse.getFileUrl())
                .fileSize((int) currentFileSize)
                .duration((int) currentDuration)
                .status((byte) 2) // 2: COMPLETED
                .endTime(now)
                .updatedAt(now)
                .build();

        roomRecordPOMapper.updateCompletedInfo(updatePO);

        // 5. 清理 Redis
        redisTemplate.delete(activeKey); // 释放互斥锁
        redisTemplate.delete(statusKey); // 删除实时状态缓存

        // 6. 异步发送 RocketMQ 消息
        // 需要 final 变量供 lambda 使用
        final long finalDuration = currentDuration;
        final long finalFileSize = currentFileSize;
        final String fileUrl = grpcResponse.getFileUrl();

        threadPoolTaskExecutor.execute(() -> {
            try {
                RecordingCompletedEventDTO eventDTO = RecordingCompletedEventDTO.builder()
                        .roomId(roomId)
                        .hostId(hostId)
                        .fileUrl(fileUrl)
                        .fileSize(finalFileSize)
                        .duration((int) finalDuration)
                        .endTime(now)
                        .timestamp(LocalDateTime.now())
                        .build();

                rocketMQTemplate.syncSend(
                        MQConstants.ROOM_RECORD_TOPIC + ":" + MQConstants.TAG_RECORDING_COMPLETED,
                        MessageBuilder.withPayload(eventDTO).build()
                );
            } catch (Exception e) {
                log.error("[RocketMQ] 发送录制完成事件失败", e);
            }
        });

        return StopRecordingResVO.builder()
                .roomId(roomId)
                .hostId(hostId)
                .status(2)
                .fileUrl(fileUrl)
                .fileSize((int) finalFileSize)
                .duration((int) finalDuration)
                .endTime(now)
                .build();
    }

    /**
     * 获取录制状态
     */
    @Override
    public GetRecordingStatusResVO getRecordingStatus(Long roomId, Long hostId) {
        String statusKey = String.format(RedisKeyConstants.ROOM_RECORDING_STATUS_KEY, roomId);

        // 1. 优先查询 Redis 缓存 (Hash 结构)
        Map<Object, Object> cachedMap = redisTemplate.opsForHash().entries(statusKey);
        if (!cachedMap.isEmpty()) {
            try {
                // 将 Map 转换为 VO
                GetRecordingStatusResVO cachedVO = MapUtils.mapToObject((Map) cachedMap, GetRecordingStatusResVO.class);

                // 2. 动态计算时长：对于进行中的录制，Redis 存的是静态数据，可以根据 startTime 算出当前 duration
                if (cachedVO.getStartTime() != null) {
                    long seconds = Duration.between(cachedVO.getStartTime(), LocalDateTime.now()).getSeconds();
                    cachedVO.setDurationSeconds((int) seconds);
                }
                cachedVO.setIsRecording(true);
                return cachedVO;
            } catch (Exception e) {
                log.warn("[GetRecordingStatus] Redis 数据解析警告，将降级查询 gRPC: {}", e.getMessage());
            }
        }

        // 3. 缓存未命中，调用 gRPC 查询实时权威状态
        try {
            RecordingStatusResponse grpcResponse = sfuGrpcClient.getRecordingStatus(String.valueOf(roomId), String.valueOf(hostId));

            boolean isRecording = grpcResponse.getIsRecording();

            GetRecordingStatusResVO.GetRecordingStatusResVOBuilder resBuilder = GetRecordingStatusResVO.builder()
                    .roomId(roomId)
                    .hostId(hostId)
                    .isRecording(isRecording)
                    .durationSeconds((int) grpcResponse.getDurationSeconds())
                    .fileSizeBytes(grpcResponse.getFileSizeBytes())
                    .status(isRecording ? 0 : 2); // 0:录制中 2:已完成

            // 4. 如果正在录制，尝试从 DB 补全 startTime 等静态元数据并回写缓存
            if (isRecording) {
                RoomRecordPO dbRecord = roomRecordPOMapper.selectActiveByRoomId(roomId);
                if (dbRecord != null) {
                    resBuilder.startTime(dbRecord.getStartTime());
                    resBuilder.format(dbRecord.getFormat());

                    // 回写缓存，防止缓存击穿（更新 TTL）
                    Map<String, String> cacheMap = MapUtils.objectToStringMap(dbRecord);
                    redisTemplate.opsForHash().putAll(statusKey, cacheMap);
                    redisTemplate.expire(statusKey, RedisKeyConstants.ROOM_RECORDING_STATUS_EXPIRE_TIME, TimeUnit.SECONDS);
                }
            } else {
                // 如果未在录制，可能是查询历史记录？
                // 根据业务逻辑，如果 gRPC 说不在录制，则返回未录制状态即可
            }
            return resBuilder.build();

        } catch (Exception e) {
            log.error("[GetRecordingStatus] SFU 查询状态失败，降级查询 DB: roomId={}", roomId, e);

            // 5. 最终兜底：查询 DB Active 记录
            RoomRecordPO dbRecord = roomRecordPOMapper.selectActiveByRoomId(roomId);
            if (dbRecord != null) {
                // DB 显示有活跃记录，但 gRPC 失败，暂时认为还在录制（或状态未知）
                return GetRecordingStatusResVO.builder()
                        .roomId(dbRecord.getRoomId())
                        .hostId(dbRecord.getHostId())
                        .isRecording(true)
                        .startTime(dbRecord.getStartTime())
                        .status(0)
                        .durationSeconds((int) Duration.between(dbRecord.getStartTime(), LocalDateTime.now()).getSeconds())
                        .build();
            }
            // 均无记录
            return GetRecordingStatusResVO.builder()
                    .roomId(roomId)
                    .hostId(hostId)
                    .isRecording(false)
                    .status(2)
                    .build();
        }
    }
}