package com.theoyu.thesis.media.biz.grpc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.grpc.proto.*;
import com.theoyu.thesis.media.biz.model.entity.RoomMessagePO;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomMessagePOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomParticipantPOMapper;
import com.theoyu.thesis.media.biz.model.vo.RoomConfigVO;
import com.theoyu.thesis.media.biz.rpc.UserRpcService;
import com.theoyu.thesis.media.biz.rpc.idGeneratorRpcService;
import com.theoyu.thesis.media.biz.util.RoomCacheHelper;
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
    private idGeneratorRpcService idGeneratorRpcService;

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

            // 5. 检查用户权限
            if (!checkUserPermission(roomId, userId, room.getHostId())) {
                sendAccessDeniedResponse(responseObserver, "无权限访问该房间");
                return;
            }

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

        try {
            String roomId = request.getRoomId();
            String userId = request.getUserId();
            String username = request.getUsername();
            long timestamp = request.getTimestamp();

            // 2. 创建参与者记录
            RoomParticipantPO participant = new RoomParticipantPO();
            LocalDateTime now = LocalDateTime.now();
            participant.setRoomId(Long.parseLong(roomId));
            participant.setUserId(Long.parseLong(userId));
            participant.setJoinedAt(now);
            participant.setRole(1); // 角色: 1-普通成员, 2-主持人, 3-联席主持
            participant.setStatus(1); // 状态: 1-在线, 2-离线(中途退出), 3-被移除
            participant.setCreatedTime(now);
            participant.setUpdatedTime(now);

            roomParticipantPOMapper.insert(participant);

            // 3. 更新 Redis 缓存
            updateParticipantCache(roomId, userId, username, true);

            // 4. 异步发送 MQ 消息
            taskExecutor.execute(() -> {
                try {
                    Map<String, Object> message = new HashMap<>();
                    message.put("roomId", roomId);
                    message.put("userId", userId);
                    message.put("username", username);
                    message.put("event", "joined");
                    message.put("timestamp", timestamp);

                    rocketMQTemplate.convertAndSend(
                            MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_PARTICIPANT_JOINED,
                            JSON.toJSONString(message)
                    );

                    log.info("[SFU-gRPC] MQ message sent - participant joined: {}", userId);
                } catch (Exception e) {
                    log.error("[SFU-gRPC] Failed to send MQ message", e);
                }
            });

            // 5. 记录房间消息
            taskExecutor.execute(() -> {
                try {
                    Long messageId = Long.valueOf(idGeneratorRpcService.getRoomMsgId());
                    RoomMessagePO message = new RoomMessagePO();
                    message.setId(messageId);
                    message.setRoomId(Long.parseLong(roomId));
                    message.setSenderId(Long.parseLong(userId));
                    message.setMessageType(1); // 1-系统消息
                    message.setContentType(1);
                    // TODO:调用KV服务存储消息内容，设置 contentUuid 字段
                    message.setCreatedTime(LocalDateTime.now());

                    roomMessagePOMapper.insert(message);
                } catch (Exception e) {
                    log.error("[SFU-gRPC] Failed to insert room message", e);
                }
            });

            // 6. 返回成功响应
            AckResponse response = AckResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("参与者加入通知处理成功")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[SFU-gRPC] notifyParticipantJoined success - roomId: {}, userId: {}", roomId, userId);

        } catch (Exception e) {
            log.error("[SFU-gRPC] notifyParticipantJoined error", e);
            sendAckErrorResponse(responseObserver, "参与者加入通知处理失败");
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

            // 1. 更新参与者状态
            roomParticipantPOMapper.updateStatusByRoomIdAndUserId(
                    Long.parseLong(roomId),
                    Long.parseLong(userId),
                    0, // 0-离线
                    LocalDateTime.now()
            );

            // 2. 清理 Redis 缓存
            updateParticipantCache(roomId, userId, username, false);

            // 3. 异步发送 MQ 消息
            taskExecutor.execute(() -> {
                try {
                    Map<String, Object> message = new HashMap<>();
                    message.put("roomId", roomId);
                    message.put("userId", userId);
                    message.put("username", username);
                    message.put("event", "left");
                    message.put("timestamp", timestamp);

                    rocketMQTemplate.convertAndSend(
                            MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_PARTICIPANT_LEFT,
                            JSON.toJSONString(message)
                    );

                    log.info("[SFU-gRPC] MQ message sent - participant left: {}", userId);
                } catch (Exception e) {
                    log.error("[SFU-gRPC] Failed to send MQ message", e);
                }
            });

            // 4. 记录房间消息
            taskExecutor.execute(() -> {
                try {
                    Long messageId = Long.valueOf(idGeneratorRpcService.getRoomMsgId());
                    RoomMessagePO message = new RoomMessagePO();
                    message.setId(messageId);
                    message.setRoomId(Long.parseLong(roomId));
                    message.setSenderId(Long.parseLong(userId));
                    message.setMessageType(1); // 1-系统消息
                    // TODO: contentUuid 字段的使用需确认，调用KV服务存储消息内容
                    message.setCreatedTime(LocalDateTime.now());

                    roomMessagePOMapper.insert(message);
                } catch (Exception e) {
                    log.error("[SFU-gRPC] Failed to insert room message", e);
                }
            });

            // 5. 返回成功响应
            AckResponse response = AckResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("参与者离开通知处理成功")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("[SFU-gRPC] notifyParticipantLeft success - roomId: {}, userId: {}", roomId, userId);

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

        log.info("[SFU-gRPC] getRoomConfig - roomId: {}", request.getRoomId());

        try {
            String roomId = request.getRoomId();

            // 1. 从缓存获取房间配置
            String configKey = String.format(RedisKeyConstants.ROOM_CONFIG_KEY, roomId);
            RoomConfigVO config = (RoomConfigVO) redisTemplate.opsForValue().get(configKey);

            if (config == null) {
                // 2. 缓存未命中，从数据库查询
                RoomPO room = roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));
                if (room == null) {
                    log.warn("[SFU-gRPC] Room not found - roomId: {}", roomId);
                    RoomConfigResponse response = RoomConfigResponse.newBuilder()
                            .setMaxParticipants(0)
                            .setEnableRecording(false)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }

                config = RoomConfigVO.builder()
                        .maxParticipants(room.getMaxParticipants())
                        .build();

                // 3. 更新缓存
                redisTemplate.opsForValue().set(
                        configKey,
                        config,
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

            log.info("[SFU-gRPC] getRoomConfig success - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[SFU-gRPC] getRoomConfig error", e);
            RoomConfigResponse response = RoomConfigResponse.newBuilder()
                    .setMaxParticipants(0)
                    .setEnableRecording(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
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
                return RoomCacheHelper.hashMapToRoom(hashMap);
            }

            // 缓存未命中，查询数据库
            RoomPO room = roomPOMapper.selectByPrimaryKey(Long.parseLong(roomId));

            if (room != null) {
                // 回写缓存
                Map<String, String> roomHashMap = RoomCacheHelper.roomToHashMap(room);
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
     * 更新参与者缓存
     */
    private void updateParticipantCache(String roomId, String userId, String username, boolean isJoin) {
        String participantsKey = String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId);
        String participantInfoKey = String.format(RedisKeyConstants.PARTICIPANT_INFO_KEY, roomId, userId);

        if (isJoin) {
            // 加入房间
            redisTemplate.opsForSet().add(participantsKey, userId);
            redisTemplate.expire(participantsKey, RedisKeyConstants.PARTICIPANT_INFO_EXPIRE_TIME, TimeUnit.SECONDS);

            Map<String, Object> participantInfo = new HashMap<>();
            participantInfo.put("userId", userId);
            participantInfo.put("username", username);
            participantInfo.put("joinTime", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(participantInfoKey, participantInfo);
            redisTemplate.expire(participantInfoKey, RedisKeyConstants.PARTICIPANT_INFO_EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            // 离开房间
            redisTemplate.opsForSet().remove(participantsKey, userId);
            redisTemplate.delete(participantInfoKey);
        }
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