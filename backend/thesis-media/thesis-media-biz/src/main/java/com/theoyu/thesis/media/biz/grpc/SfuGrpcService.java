package com.theoyu.thesis.media.biz.grpc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.framework.common.utils.MapUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.grpc.proto.*;
import com.theoyu.thesis.media.biz.model.dto.ParticipantEventDTO;
import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomMessagePOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomParticipantPOMapper;
import com.theoyu.thesis.media.biz.model.vo.RoomConfigVO;
import com.theoyu.thesis.media.biz.rpc.UserRpcService;
import com.theoyu.thesis.media.biz.rpc.IdGeneratorRpcService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SFU gRPC 服务实现
 *
 * @author theoyu
 */
@Slf4j
@GrpcService
public class SfuGrpcService extends SFUServiceGrpc.SFUServiceImplBase {

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource
    private RoomParticipantPOMapper roomParticipantPOMapper;

    @Resource
    private RoomMessagePOMapper roomMessagePOMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 验证房间访问权限
     */
    @Override
    public void validateRoomAccess(
            RoomAccessRequest request,
            StreamObserver<RoomAccessResponse> responseObserver) {

        log.info("[SFU-gRPC] validateRoomAccess - roomId: {}, userId: {}",
                request.getRoomId(), request.getUserId());

        try {
            String roomId = request.getRoomId();
            String userId = request.getUserId();
            String token = request.getToken();

            // TODO:1. 验证 Token


            // 2. 检查房间是否存在
            RoomPO room = getRoomFromCacheOrDB(roomId);
            if (room == null) {
                sendAccessDeniedResponse(responseObserver, "房间不存在");
                return;
            }

            // 3. 检查房间状态
            if (room.getStatus() != 1) {
                sendAccessDeniedResponse(responseObserver, "房间已关闭");
                return;
            }

            // 4. 检查房间人数限制
            Integer participantCount = this.roomParticipantPOMapper.countByRoomIdAndStatus(Long.valueOf(roomId), 1);
            if (participantCount >= room.getMaxParticipants()) {
                sendAccessDeniedResponse(responseObserver, "房间已满");
                return;
            }

            // 5. 检查用户权限(需要重新设计校验逻辑)
//            if (!checkUserPermission(roomId, userId, room.getHostId())) {
//                sendAccessDeniedResponse(responseObserver, "无权限访问该房间");
//                return;
//            }

            // 6. 构建房间配置
            RoomConfig roomConfig = RoomConfig.newBuilder()
                    .setMaxParticipants(room.getMaxParticipants())
                    .build();

            // 7. 返回成功响应
            RoomAccessResponse response = RoomAccessResponse.newBuilder()
                    .setAllowed(true)
                    .setMessage("访问权限验证成功")
                    .setConfig(roomConfig)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[SFU-gRPC] validateRoomAccess success - roomId: {}, userId: {}", roomId, userId);

        } catch (Exception e) {
            log.error("[SFU-gRPC] validateRoomAccess error", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("系统异常: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    /**
     * 通知参与者加入
     */
    @Override
    public void notifyParticipantJoined(
            ParticipantEvent request,
            StreamObserver<AckResponse> responseObserver) {

        log.info("[SFU-gRPC] notifyParticipantJoined - roomId: {}, userId: {}, username: {}",
                request.getRoomId(), request.getUserId(), request.getUsername());
        String roomId = request.getRoomId();
        String userId = request.getUserId();
        String username = request.getUsername();
        long timestamp = request.getTimestamp();

        try {
            Long roomIdLong = Long.parseLong(roomId);
            Long userIdLong = Long.parseLong(userId);
            LocalDateTime now = LocalDateTime.now();

            // 1. 使用 insertOrUpdate 方法处理参与者记录
            RoomParticipantPO participant = RoomParticipantPO.builder()
                    .roomId(roomIdLong)
                    .userId(userIdLong)
                    .joinedAt(now)
                    .audioMuted(true)
                    .videoMuted(true)
                    .role(1) // 1-普通成员
                    .status(1) // 1-在线
                    .createdTime(now)
                    .updatedTime(now)
                    .build();

            roomParticipantPOMapper.insertOrUpdate(participant);
            log.info("[SFU-gRPC] 参与者记录已更新 - userId: {}", userId);

            // 2. 缓存参与者信息到 Redis
            cacheParticipantInfo(roomId, userId, username, timestamp);

            // 3. 异步发送 MQ 消息
            sendParticipantEventToMQ(roomId, userId, username, "joined", timestamp);

            // 4. 异步记录房间系统消息
            recordSystemMessage(roomIdLong, userIdLong, username, "加入了房间");

            // 5. 返回成功响应
            sendAckSuccessResponse(responseObserver, "加入房间成功");

        } catch (NumberFormatException e) {
            log.error("[SFU-gRPC] 参数格式错误 - roomId: {}, userId: {}", roomId, userId, e);
            sendAckErrorResponse(responseObserver, "参数格式错误");
        } catch (Exception e) {
            log.error("[SFU-gRPC] 参与者加入处理异常", e);
            sendAckErrorResponse(responseObserver, "加入房间失败,请稍后重试");
        }
    }



    /**
     * 通知参与者离开
     */
    @Override
    public void notifyParticipantLeft(
            ParticipantEvent request,
            StreamObserver<AckResponse> responseObserver) {

        log.info("[SFU-gRPC] notifyParticipantLeft - roomId: {}, userId: {}, username: {}",
                request.getRoomId(), request.getUserId(), request.getUsername());

        try {
            String roomId = request.getRoomId();
            String userId = request.getUserId();
            String username = request.getUsername();
            long timestamp = request.getTimestamp();

            // 1. 更新参与者离线状态
            roomParticipantPOMapper.updateStatusByRoomIdAndUserId(
                    Long.parseLong(roomId),
                    Long.parseLong(userId),
                    LocalDateTime.now()
            );

            // 2. 清理 Redis 缓存
            removeParticipantCache(roomId, userId);

            // 3. 异步发送 MQ 消息
            sendParticipantEventToMQ(roomId, userId, username, "left", timestamp);

            // 4. 记录房间消息
            recordSystemMessage(
                    Long.parseLong(roomId),
                    Long.parseLong(userId),
                    username,
                    "离开了房间"
            );

            // 5. 返回成功响应
            sendAckSuccessResponse(responseObserver, "参与者离开通知处理成功");

            log.info("[SFU-gRPC] 参与者离开处理完成 - userId: {}", userId);


        } catch (Exception e) {
            log.error("[SFU-gRPC] notifyParticipantLeft error", e);
            sendAckErrorResponse(responseObserver, "参与者离开通知处理失败");
        }
    }

    /**
     * 获取房间配置
     */
    @Override
    public void getRoomConfig(
            RoomConfigRequest request,
            StreamObserver<RoomConfigResponse> responseObserver) {

        log.info("[SFU-gRPC] 获取房间配置 - roomId: {}", request.getRoomId());

        try {
            String roomId = request.getRoomId();

            // 1. 从缓存获取房间配置 (使用 Hash 结构)
            String configKey = String.format(RedisKeyConstants.ROOM_CONFIG_KEY, roomId);
            Map<Object, Object> configMap = redisTemplate.opsForHash().entries(configKey);

            RoomConfigVO config;
            if (!configMap.isEmpty()) {
                // 使用 MapUtils 转换
                config = MapUtils.mapToObject(configMap, RoomConfigVO.class);
            } else {
                // 2. 缓存未命中,从数据库查询
                RoomPO room = roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));
                if (room == null) {
                    log.warn("[SFU-gRPC] 房间不存在 - roomId: {}", roomId);
                    sendDefaultRoomConfigResponse(responseObserver);
                    return;
                }

                config = RoomConfigVO.builder()
                        .maxParticipants(room.getMaxParticipants())
                        .enableRecording(false)
                        .build();

                // 3. 使用 MapUtils 转换并更新缓存
                Map<String, String> configHashMap = MapUtils.objectToStringMap(config);
                redisTemplate.opsForHash().putAll(configKey, configHashMap);
                redisTemplate.expire(
                        configKey,
                        RedisKeyConstants.ROOM_CONFIG_EXPIRE_TIME,
                        TimeUnit.SECONDS
                );
            }

            // 4. 返回配置
            RoomConfigResponse response = RoomConfigResponse.newBuilder()
                    .setMaxParticipants(config.getMaxParticipants())
                    .setEnableRecording(config.getEnableRecording())
                    .addAllAllowedCodecs(config.getAllowedCodecs())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[SFU-gRPC] 房间配置获取成功 - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[SFU-gRPC] 获取房间配置异常", e);
            sendDefaultRoomConfigResponse(responseObserver);
        }
    }

    /**
     * 上报媒体统计信息
     */
    @Override
    public void reportMediaStats(
            MediaStatsRequest request,
            StreamObserver<AckResponse> responseObserver) {

        log.info("[SFU-gRPC] reportMediaStats - roomId: {}, peerId: {}",
                request.getRoomId(), request.getPeerId());

        try {
            String roomId = request.getRoomId();
            String peerId = request.getPeerId();
            Map<String, String> stats = request.getStatsMap();

            // 1. 存储到 Redis（时序数据）
            String timestamp = String.valueOf(System.currentTimeMillis());
            String statsKey = String.format(RedisKeyConstants.MEDIA_STATS_KEY, roomId, peerId, timestamp);

            redisTemplate.opsForValue().set(
                    statsKey,
                    JSONUtil.toJsonStr(stats),
                    RedisKeyConstants.MEDIA_STATS_EXPIRE_TIME,
                    TimeUnit.SECONDS
            );

            // 2. 异步发送到 MQ 进行数据分析
            taskExecutor.execute(() -> {
                try {
                    Map<String, Object> message = new HashMap<>();
                    message.put("roomId", roomId);
                    message.put("peerId", peerId);
                    message.put("stats", stats);
                    message.put("timestamp", timestamp);

                    rocketMQTemplate.convertAndSend(
                            MQConstants.TOPIC_MEDIA_STATS + ":" + MQConstants.TAG_STATS_REPORT,
                            JSON.toJSONString(message)
                    );

                    log.info("[SFU-gRPC] Media stats sent to MQ - peerId: {}", peerId);
                } catch (Exception e) {
                    log.error("[SFU-gRPC] Failed to send media stats to MQ", e);
                }
            });

            // 3. 返回成功响应
            AckResponse response = AckResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("媒体统计数据上报成功")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("[SFU-gRPC] reportMediaStats success - peerId: {}", peerId);

        } catch (Exception e) {
            log.error("[SFU-gRPC] reportMediaStats error", e);
            sendAckErrorResponse(responseObserver, "媒体统计数据上报失败");
        }
    }

    // ==================== 私有辅助方法 ====================

    private void sendAckSuccessResponse(
            StreamObserver<AckResponse> responseObserver,
            String message) {
        AckResponse response = AckResponse.newBuilder()
                .setSuccess(true)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendDefaultRoomConfigResponse(
            StreamObserver<RoomConfigResponse> responseObserver) {
        RoomConfigResponse response = RoomConfigResponse.newBuilder()
                .setMaxParticipants(0)
                .setEnableRecording(false)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void cacheParticipantInfo(String roomId, String userId,
                                      String username, long timestamp) {
        // Hash 存储参与者详细信息
        String participantInfoKey = String.format(
                RedisKeyConstants.PARTICIPANT_INFO_KEY,
                roomId,
                userId
        );

        Map<String, Object> participantInfo = new HashMap<>();
        participantInfo.put("userId", userId);
        participantInfo.put("username", username);
        participantInfo.put("joinTime", timestamp);
        participantInfo.put("audioMuted", true);
        participantInfo.put("videoMuted", true);

        redisTemplate.opsForHash().putAll(participantInfoKey, participantInfo);
        redisTemplate.expire(
                participantInfoKey,
                RedisKeyConstants.PARTICIPANT_INFO_EXPIRE_TIME,
                TimeUnit.SECONDS
        );

        // Set 存储房间参与者列表
        String participantsSetKey = String.format(
                RedisKeyConstants.ROOM_PARTICIPANTS_KEY,
                roomId
        );
        redisTemplate.opsForSet().add(participantsSetKey, userId);
        redisTemplate.expire(
                participantsSetKey,
                RedisKeyConstants.PARTICIPANT_INFO_EXPIRE_TIME,
                TimeUnit.SECONDS
        );

        log.debug("[SFU-gRPC] 参与者信息已缓存 - userId: {}", userId);
    }

    /**
     * 发送参与者事件到 MQ
     */
    private void sendParticipantEventToMQ(String roomId, String userId,
                                          String username, String eventType,
                                          long timestamp) {
        taskExecutor.execute(() -> {
            try {
                ParticipantEventDTO eventDTO = ParticipantEventDTO.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .username(username)
                        .eventType(eventType)
                        .timestamp(timestamp)
                        .build();

                String destination = MQConstants.TOPIC_PARTICIPANT_EVENT + ":"
                        + (eventType.equals("joined")
                        ? MQConstants.TAG_PARTICIPANT_JOINED
                        : MQConstants.TAG_PARTICIPANT_LEFT);

                rocketMQTemplate.convertAndSend(
                        destination,
                        JsonUtils.toJsonString(eventDTO)
                );

                log.info("[SFU-gRPC] MQ消息发送成功 - 事件类型: {}, userId: {}",
                        eventType, userId);
            } catch (Exception e) {
                log.error("[SFU-gRPC] MQ消息发送失败 - eventType: {}", eventType, e);
            }
        });
    }


    /**
     * 记录房间系统消息
     */
    private void recordSystemMessage(Long roomId, Long userId,
                                     String username, String action) {
        taskExecutor.execute(() -> {
            try {
                Long messageId = Long.valueOf(idGeneratorRpcService.getRoomMsgId());

                RoomMessagePO message = RoomMessagePO.builder()
                        .id(messageId)
                        .roomId(roomId)
                        .senderId(userId)
                        .messageType(1) // 1-系统消息
                        .contentType(1) // 1-文本
                        .content("用户 " + username + " " + action)
                        .createdTime(LocalDateTime.now())
                        .build();

                roomMessagePOMapper.insert(message);
                log.info("[SFU-gRPC] 房间消息记录成功 - messageId: {}", messageId);
            } catch (Exception e) {
                log.error("[SFU-gRPC] 房间消息记录失败", e);
            }
        });
    }


    /**
     * 从缓存或数据库获取房间信息
     */
    private RoomPO getRoomFromCacheOrDB(String roomId) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);

            // 从 Redis Hash 获取
            Map<Object, Object> hashMap = redisTemplate.opsForHash().entries(roomKey);

            if (!hashMap.isEmpty()) {
                // 从 Hash 转换为 RoomPO
                return MapUtils.mapToObject(hashMap, RoomPO.class);
            }

            // 缓存未命中，查询数据库
            RoomPO room = roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));

            if (room != null) {
                // 回写缓存
                Map<String, String> roomHashMap = MapUtils.objectToStringMap(room);
                redisTemplate.opsForHash().putAll(roomKey, roomHashMap);
                redisTemplate.expire(roomKey,
                        RedisKeyConstants.ROOM_INFO_EXPIRE_TIME,
                        TimeUnit.SECONDS);
            }

            return room;

        } catch (Exception e) {
            log.error("[SFU-gRPC] Failed to get room from cache", e);
            // 降级：直接查询数据库
            return roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));
        }
    }

    /**
     * 检查用户权限
     */
    private boolean checkUserPermission(String roomId, String userId, Long creatorId) {
        // 1. 检查是否是房间创建者
        if (creatorId.equals(Long.parseLong(userId))) {
            return true;
        }

        // 2. 检查是否在参与者列表中（之前已加入过）
        RoomParticipantPO participant = roomParticipantPOMapper.selectByRoomIdAndUserId(
                Long.parseLong(roomId),
                Long.parseLong(userId)
        );

        return participant != null;
    }

    /**
     * 解析允许的编解码器列表
     */
    private List<String> parseAllowedCodecs(String allowedCodecs) {
        if (StrUtil.isBlank(allowedCodecs)) {
            return Arrays.asList("opus", "VP8", "VP9", "h264");
        }
        return Arrays.asList(allowedCodecs.split(","));
    }

    /**
     * 移除参与者缓存
     */
    private void removeParticipantCache(String roomId, String userId) {
        // 删除 Hash 中的参与者详细信息
        String participantInfoKey = String.format(
                RedisKeyConstants.PARTICIPANT_INFO_KEY,
                roomId,
                userId
        );
        redisTemplate.delete(participantInfoKey);

        // 从 Set 中移除参与者ID
        String participantsSetKey = String.format(
                RedisKeyConstants.ROOM_PARTICIPANTS_KEY,
                roomId
        );
        redisTemplate.opsForSet().remove(participantsSetKey, userId);

        log.debug("[SFU-gRPC] 参与者缓存已清理 - userId: {}", userId);
    }

    /**
     * 发送访问拒绝响应
     */
    private void sendAccessDeniedResponse(
            StreamObserver<RoomAccessResponse> responseObserver,
            String message) {
        RoomAccessResponse response = RoomAccessResponse.newBuilder()
                .setAllowed(false)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * 发送确认错误响应
     */
    private void sendAckErrorResponse(
            StreamObserver<AckResponse> responseObserver,
            String message) {
        AckResponse response = AckResponse.newBuilder()
                .setSuccess(false)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * 脱敏 Token
     */
    private String maskToken(String token) {
        if (StrUtil.isBlank(token) || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 5) + "***" + token.substring(token.length() - 5);
    }
}