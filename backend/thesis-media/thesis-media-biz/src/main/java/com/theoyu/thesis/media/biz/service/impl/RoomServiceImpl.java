package com.theoyu.thesis.media.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.grpc.SfuGrpcService;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.entity.RoomParticipantPO;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import com.theoyu.thesis.media.biz.model.mapper.RoomParticipantPOMapper;
import com.theoyu.thesis.media.biz.model.vo.*;
import com.theoyu.thesis.media.biz.rpc.UserRpcService;
import com.theoyu.thesis.media.biz.rpc.idGeneratorRpcService;
import com.theoyu.thesis.media.biz.service.RoomService;
import com.theoyu.thesis.media.biz.util.RoomCacheHelper;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource
    private RoomParticipantPOMapper roomParticipantPOMapper;

    @Resource
    private idGeneratorRpcService idGeneratorRpcService;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Value("${sfu.server.url:ws://localhost:3000}")
    private String sfuServerUrl;

    private static final Integer MAX_ACTIVE_ROOMS_PER_USER = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateRoomResVO createRoom(CreateRoomReqVO reqVO) {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("[RoomService] createRoom - userId: {}, title: {}", userId, reqVO.getTitle());

        // 1. 检查用户创建会议配额
        checkRoomQuota(userId);

        // 2. 生成会议ID和会议号
        Long roomId = Long.valueOf(idGeneratorRpcService.getRoomId());
        String roomNo = generateUniqueRoomNo();

        // 3. 创建会议记录
        LocalDateTime now = LocalDateTime.now();
        RoomPO room = RoomPO.builder()
                .id(roomId)
                .sfuNodeId(reqVO.getSfuNodeId())
                .roomNo(roomNo)
                .hostId(userId)
                .title(reqVO.getTitle())
                .type(reqVO.getType())
                .maxParticipants(reqVO.getMaxParticipants())
                .status(1)
                .startTime(now)
                .settings(reqVO.getSettings())
                .createdTime(now)
                .updatedTime(now)
                .build();

        roomPOMapper.insert(room);

        // 4. 缓存会议信息（使用 Hash 结构）
        cacheRoomInfo(room);

        // 5. 异步发送会议创建事件到MQ
        threadPoolTaskExecutor.execute(() -> {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("roomId", roomId);
                message.put("roomNo", roomNo);
                message.put("hostId", userId);
                message.put("title", reqVO.getTitle());
                message.put("timestamp", System.currentTimeMillis());

                rocketMQTemplate.convertAndSend(
                        MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_ROOM_CREATED,
                        JSON.toJSONString(message)
                );

                log.info("[RoomService] Room created event sent to MQ - roomId: {}", roomId);
            } catch (Exception e) {
                log.error("[RoomService] Failed to send room created event", e);
            }
        });

        // 6. 构建响应
        return CreateRoomResVO.builder()
                .roomId(roomId)
                .roomNo(roomNo)
                .title(room.getTitle())
                .sfuServerUrl(sfuServerUrl)
                .maxParticipants(room.getMaxParticipants())
                .createdTime(room.getCreatedTime())
                .build();
    }

    @Override
    public GetRoomInfoResVO getRoomInfo(Long roomId) {
        log.info("[RoomService] getRoomInfo - roomId: {}", roomId);

        // 1. 从缓存获取会议信息（Hash 优化）
        RoomPO room = getRoomFromCache(roomId);

        if (room == null) {
            throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
        }

        // 2. 获取房主信息
        FindUserByIdRspDTO hostInfo = userRpcService.findById(room.getHostId());

        // 3. 获取当前参与者数量
        Integer currentParticipants = getCurrentParticipantCount(roomId);

        // 4. 获取在线参与者列表
        List<GetRoomInfoResVO.ParticipantInfoVO> participants = getOnlineParticipants(roomId);

        // 5. 构建响应
        return GetRoomInfoResVO.builder()
                .roomId(room.getId())
                .roomNo(room.getRoomNo())
                .title(room.getTitle())
                .hostId(room.getHostId())
                .hostName(hostInfo != null ? hostInfo.getNickName() : "未知")
                .type(room.getType())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .status(room.getStatus())
                .startTime(room.getStartTime())
                .createdTime(room.getCreatedTime())
                .sfuServerUrl(sfuServerUrl)
                .participants(participants)
                .build();
    }

    /**
     * 加入会议（预验证阶段）
     * 
     * 此方法不会执行持久化操作。真正的参与者记录将在用户成功连接到 SFU 服务器后，
     * 由 SFU 通过 gRPC 调用 {@link SfuGrpcService#notifyParticipantJoined} 时创建。
     * 这样可以避免"幽灵参与者"问题（用户调用 API 但未实际连接）。
     * 
     * @param reqVO 加入会议请求
     * @return 验证结果，包含 SFU 服务器地址
     */
    @Override
    public JoinRoomResVO joinRoom(JoinRoomReqVO reqVO) {
        Long userId = LoginUserContextHolder.getUserId();
        Long roomId = reqVO.getRoomId();

        log.info("[RoomService] joinRoom - userId: {}, roomId: {}", userId, roomId);

        // 1. 检查会议是否存在
        RoomPO room = getRoomFromCache(roomId);
        if (room == null) {
            return JoinRoomResVO.builder()
                    .allowed(false)
                    .message("会议不存在")
                    .build();
        }

        // 2. 检查会议状态
        if (room.getStatus() == 2) {
            return JoinRoomResVO.builder()
                    .allowed(false)
                    .message("会议已结束")
                    .build();
        }
        if (room.getStatus() == 3) {
            return JoinRoomResVO.builder()
                    .allowed(false)
                    .message("会议已取消")
                    .build();
        }

        // 3. 检查会议人数限制
        Integer currentCount = getCurrentParticipantCount(roomId);
        if (currentCount >= room.getMaxParticipants()) {
            return JoinRoomResVO.builder()
                    .allowed(false)
                    .message("会议已满")
                    .build();
        }

        // 4. 检查用户权限
        if (!checkUserPermission(roomId, userId, room.getHostId())) {
            return JoinRoomResVO.builder()
                    .allowed(false)
                    .message("无权限访问该会议")
                    .build();
        }

        // 5. 返回成功响应
        return JoinRoomResVO.builder()
                .roomId(roomId)
                .sfuServerUrl(sfuServerUrl)
                .allowed(true)
                .message("验证成功")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRoom(Long roomId) {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("[RoomService] closeRoom - userId: {}, roomId: {}", userId, roomId);

        // 1. 查询会议
        RoomPO room = getRoomFromCache(roomId);
        if (room == null) {
            throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
        }

        // 2. 检查权限
        if (!room.getHostId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.ROOM_ACCESS_DENIED);
        }

        // 3. 更新数据库会议状态
        roomPOMapper.updateStatusById(roomId, 2);

        // 4. 更新缓存中的会议状态（Hash 原子更新）
        updateRoomStatusInCache(roomId, 2);

        // 5. 异步发送会议关闭事件
        threadPoolTaskExecutor.execute(() -> {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("roomId", roomId);
                message.put("hostId", userId);
                message.put("timestamp", System.currentTimeMillis());

                rocketMQTemplate.convertAndSend(
                        MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_ROOM_CLOSED,
                        JSON.toJSONString(message)
                );

                log.info("[RoomService] Room closed event sent to MQ - roomId: {}", roomId);
            } catch (Exception e) {
                log.error("[RoomService] Failed to send room closed event", e);
            }
        });
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 缓存会议信息（使用 Hash 结构）
     */
    private void cacheRoomInfo(RoomPO room) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, room.getId());

            // 将 RoomPO 转换为 Hash Map
            Map<String, String> hashMap = RoomCacheHelper.roomToHashMap(room);

            // 存储到 Redis Hash
            redisTemplate.opsForHash().putAll(roomKey, hashMap);

            // 设置过期时间
            redisTemplate.expire(roomKey,
                    RedisKeyConstants.ROOM_INFO_EXPIRE_TIME,
                    TimeUnit.SECONDS);

            // 缓存会议号映射
            String roomNoKey = String.format(RedisKeyConstants.ROOM_NO_KEY, room.getRoomNo());
            redisTemplate.opsForValue().set(roomNoKey, room.getId().toString());

            log.debug("[RoomService] Cached room info to Hash - roomId: {}", room.getId());

        } catch (Exception e) {
            log.error("[RoomService] Failed to cache room info - roomId: {}", room.getId(), e);
        }
    }

    /**
     * 从缓存获取会议信息（Hash 优化版）
     */
    private RoomPO getRoomFromCache(Long roomId) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);

            // 从 Redis Hash 获取所有字段
            Map<Object, Object> hashMap = redisTemplate.opsForHash().entries(roomKey);

            if (hashMap.isEmpty()) {
                // 缓存未命中，从DB查询
                log.debug("[RoomService] Cache miss, query from DB - roomId: {}", roomId);
                RoomPO room = roomPOMapper.selectByPrimaryKey(roomId);

                if (room != null) {
                    // 回写缓存
                    cacheRoomInfo(room);
                }

                return room;
            }

            // 从 Hash Map 转换为 RoomPO
            return RoomCacheHelper.hashMapToRoom(hashMap);

        } catch (Exception e) {
            log.error("[RoomService] Failed to get room from cache - roomId: {}", roomId, e);
            // 降级：直接查询数据库
            return roomPOMapper.selectByPrimaryKey(roomId);
        }
    }

    /**
     * 更新缓存中的会议状态（Hash 原子更新）
     */
    private void updateRoomStatusInCache(Long roomId, Integer status) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);

            // Hash 字段级更新（原子操作）
            redisTemplate.opsForHash().put(roomKey, "status", String.valueOf(status));

            // 更新 updatedTime
            long currentTimestamp = System.currentTimeMillis();
            redisTemplate.opsForHash().put(roomKey, "updatedTime", String.valueOf(currentTimestamp));

            log.debug("[RoomService] Updated room status in cache - roomId: {}, status: {}",
                    roomId, status);

        } catch (Exception e) {
            log.error("[RoomService] Failed to update room status in cache - roomId: {}",
                    roomId, e);
        }
    }

    // ==================== 其他方法保持不变 ====================

    private void checkRoomQuota(Long userId) {
        String quotaKey = String.format(RedisKeyConstants.USER_ROOM_QUOTA_KEY, userId);
        Integer cachedCount = (Integer) redisTemplate.opsForValue().get(quotaKey);

        if (cachedCount == null) {
            cachedCount = roomPOMapper.countActiveRoomsByHostId(userId);
            redisTemplate.opsForValue().set(quotaKey, cachedCount,
                    RedisKeyConstants.USER_ROOM_QUOTA_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        if (cachedCount >= MAX_ACTIVE_ROOMS_PER_USER) {
            throw new BusinessException(ResponseCodeEnum.ROOM_QUOTA_EXCEEDED);
        }
    }

    private String generateUniqueRoomNo() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String roomNo = RandomUtil.randomNumbers(6);
            String roomNoKey = String.format(RedisKeyConstants.ROOM_NO_KEY, roomNo);
            Boolean exists = redisTemplate.hasKey(roomNoKey);

            if (Boolean.FALSE.equals(exists)) {
                redisTemplate.opsForValue().set(roomNoKey, "1", 10, TimeUnit.MINUTES);
                return roomNo;
            }
        }

        throw new BusinessException(ResponseCodeEnum.ROOM_NO_DUPLICATE);
    }

    private Integer getCurrentParticipantCount(Long roomId) {
        String participantsKey = String.format(RedisKeyConstants.ROOM_PARTICIPANTS_KEY, roomId);
        Long redisCount = redisTemplate.opsForSet().size(participantsKey);

        if (redisCount == null) {
            return roomParticipantPOMapper.countByRoomIdAndStatus(roomId, 1);
        }

        return Math.toIntExact(redisCount);
    }

    private List<GetRoomInfoResVO.ParticipantInfoVO> getOnlineParticipants(Long roomId) {
        List<RoomParticipantPO> participants =
                roomParticipantPOMapper.selectByRoomIdAndStatus(roomId, 1);

        if (participants.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = participants.stream()
                .map(RoomParticipantPO::getUserId)
                .collect(Collectors.toList());

        List<FindUserByIdRspDTO> userInfos = userRpcService.findByIds(userIds);
        Map<Long, FindUserByIdRspDTO> userInfoMap = userInfos != null
                ? userInfos.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, u -> u))
                : new HashMap<>();

        return participants.stream().map(p -> {
            FindUserByIdRspDTO userInfo = userInfoMap.get(p.getUserId());
            return GetRoomInfoResVO.ParticipantInfoVO.builder()
                    .userId(p.getUserId())
                    .username(userInfo != null ? userInfo.getNickName() : "未知用户")
                    .avatar(userInfo != null ? userInfo.getAvatar() : "")
                    .role(p.getRole())
                    .audioMuted(p.getAudioMuted())
                    .videoMuted(p.getVideoMuted())
                    .joinedAt(p.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private boolean checkUserPermission(Long roomId, Long userId, Long hostId) {
        if (hostId.equals(userId)) {
            return true;
        }

        RoomParticipantPO participant =
                roomParticipantPOMapper.selectByRoomIdAndUserId(roomId, userId);

        return participant != null;
    }
}