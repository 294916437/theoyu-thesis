package com.theoyu.thesis.media.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.PageResponse;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.framework.common.utils.MapUtils;
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
import com.theoyu.thesis.media.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.media.biz.service.RoomService;
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
    private IdGeneratorRpcService idGeneratorRpcService;

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
    public GetRoomInfoResVO getRoomInfo(String roomIdOrNo) {
        log.info("[RoomService] getRoomInfo - roomIdOrNo: {}", roomIdOrNo);

        Long roomId = null;

        // 1. 判断入参是 roomId 还是 roomNo
        try {
            // 尝试解析为 Long，如果成功则认为是 roomId
            roomId = Long.parseLong(roomIdOrNo);
        } catch (NumberFormatException e) {
            // 从 Hash 中直接获取 roomId
            String mappingKey = RedisKeyConstants.ROOM_NO_MAPPING_KEY;
            Object cachedRoomId = redisTemplate.opsForHash().get(mappingKey, roomIdOrNo);

            if (cachedRoomId != null) {
                roomId = Long.parseLong(cachedRoomId.toString());
                log.debug("[RoomService] Found roomId from Hash cache: {}", roomId);
            } else {
                // 缓存未命中，从数据库查询
                RoomPO room = roomPOMapper.selectByRoomNo(roomIdOrNo);
                if (room != null) {
                    roomId = room.getId();

                    // 回写缓存到 Hash
                    redisTemplate.opsForHash().put(mappingKey, roomIdOrNo, roomId.toString());

                    // 设置 Hash 的过期时间（首次写入时）
                    if (!Boolean.TRUE.equals(redisTemplate.hasKey(mappingKey))) {
                        redisTemplate.expire(mappingKey,
                                RedisKeyConstants.ROOM_NO_MAPPING_EXPIRE_TIME,
                                TimeUnit.SECONDS);
                    }

                    log.debug("[RoomService] Found roomId from DB and cached to Hash: {}", roomId);
                }
            }

            if (roomId == null) {
                throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
            }
        }

        // 2. 从缓存获取会议信息（Hash 优化）
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

    @Override
    public PageResponse<RecentRoomResVO> getRecentRooms(Long page, Long size) {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("[RoomService] 获取最近参加的会议 - userId: {}, page: {}, size: {}", userId, page, size);

        // 参数校验
        if (page == null || page < 1) {
            page = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }

        try {
            // 1. 尝试从缓存获取（使用 Sorted Set 按时间排序）
            String cacheKey = String.format(RedisKeyConstants.USER_RECENT_ROOMS_KEY, userId);
            Long cacheSize = redisTemplate.opsForZSet().size(cacheKey);

            List<RecentRoomResVO> resultList;
            long totalCount;

            if (cacheSize != null && cacheSize > 0) {
                // 缓存命中：使用 ZSet 的范围查询
                long start = PageResponse.getOffset(page, size);
                long end = start + size - 1;

                // 按分数倒序获取（最近的在前）
                Set<Object> cachedRoomIds = redisTemplate.opsForZSet()
                        .reverseRange(cacheKey, start, end);

                if (cachedRoomIds != null && !cachedRoomIds.isEmpty()) {
                    resultList = buildRecentRoomsFromCache(cachedRoomIds, userId);
                    totalCount = cacheSize;
                    log.debug("[RoomService] 缓存命中 - userId: {}, count: {}", userId, totalCount);
                } else {
                    resultList = Collections.emptyList();
                }
            } else {
                // 缓存未命中：查询数据库
                log.debug("[RoomService] 缓存未命中，查询数据库 - userId: {}", userId);

                long offset = PageResponse.getOffset(page, size);
                List<RoomParticipantPO> participants = roomParticipantPOMapper
                        .selectRecentRoomsByUserId(userId, offset, size);


                if (!participants.isEmpty()) {
                    resultList = buildRecentRoomsFromDB(participants);

                    // 异步回写缓存
                    asyncCacheRecentRooms(userId, participants);
                } else {
                    resultList = Collections.emptyList();
                }
            }

            return PageResponse.success(resultList, page,size);

        } catch (Exception e) {
            log.error("[RoomService] 获取最近参加的会议失败 - userId: {}", userId, e);
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public PageResponse<UpcomingRoomResVO> getUpcomingRooms(Long page, Long size) {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("[RoomService] 获取即将开始的会议 - userId: {}, page: {}, size: {}", userId, page, size);

        // 参数校验
        if (page == null || page < 1) {
            page = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }

        try {
            // 1. 尝试从缓存获取（使用 Sorted Set 按计划开始时间排序）
            String cacheKey = String.format(RedisKeyConstants.USER_UPCOMING_ROOMS_KEY, userId);
            Long cacheSize = redisTemplate.opsForZSet().size(cacheKey);

            List<UpcomingRoomResVO> resultList;
            long totalCount;

            if (cacheSize != null && cacheSize > 0) {
                // 缓存命中：使用 ZSet 的范围查询
                long start = PageResponse.getOffset(page, size);
                long end = start + size - 1;

                // 按分数正序获取（最近开始的在前）
                Set<Object> cachedRoomIds = redisTemplate.opsForZSet()
                        .range(cacheKey, start, end);

                if (cachedRoomIds != null && !cachedRoomIds.isEmpty()) {
                    resultList = buildUpcomingRoomsFromCache(cachedRoomIds, userId);
                    totalCount = cacheSize;
                    log.debug("[RoomService] 缓存命中 - userId: {}, count: {}", userId, totalCount);
                } else {
                    resultList = Collections.emptyList();
                    totalCount = 0;
                }
            } else {
                // 缓存未命中：查询数据库
                log.debug("[RoomService] 缓存未命中，查询数据库 - userId: {}", userId);

                long offset = PageResponse.getOffset(page, size);
                List<RoomPO> rooms = roomPOMapper
                        .selectUpcomingRoomsByUserId(userId, offset, size);

                if (!rooms.isEmpty()) {
                    resultList = buildUpcomingRoomsFromDB(rooms, userId);

                    // 异步回写缓存
                    asyncCacheUpcomingRooms(userId, rooms);
                } else {
                    resultList = Collections.emptyList();
                }
            }

            return PageResponse.success(resultList, page, size);

        } catch (Exception e) {
            log.error("[RoomService] 获取即将开始的会议失败 - userId: {}", userId, e);
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    // ==================== 私有辅助方法 ====================
    /**
     * 异步缓存最近参加的会议
     * 使用 Sorted Set，score 为参与时间戳（越大越新）
     */
    private void asyncCacheRecentRooms(Long userId, List<RoomParticipantPO> participants) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                String cacheKey = String.format(RedisKeyConstants.USER_RECENT_ROOMS_KEY, userId);

                // 批量添加到 ZSet
                for (RoomParticipantPO participant : participants) {
                    double score = participant.getJoinedAt() != null
                            ? participant.getJoinedAt().toEpochSecond(java.time.ZoneOffset.of("+8"))
                            : System.currentTimeMillis() / 1000.0;

                    redisTemplate.opsForZSet().add(cacheKey, participant.getRoomId(), score);
                }

                // 设置过期时间
                redisTemplate.expire(cacheKey,
                        RedisKeyConstants.USER_RECENT_ROOMS_EXPIRE_TIME,
                        TimeUnit.SECONDS);

                // 保留最新的 100 条记录
                redisTemplate.opsForZSet().removeRange(cacheKey, 0, -101);

                log.debug("[RoomService] 异步缓存最近参加的会议成功 - userId: {}", userId);

            } catch (Exception e) {
                log.error("[RoomService] 异步缓存最近参加的会议失败 - userId: {}", userId, e);
            }
        });
    }

    /**
     * 异步缓存即将开始的会议
     * 使用 Sorted Set，score 为计划开始时间戳（越小越早）
     */
    private void asyncCacheUpcomingRooms(Long userId, List<RoomPO> rooms) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                String cacheKey = String.format(RedisKeyConstants.USER_UPCOMING_ROOMS_KEY, userId);

                // 批量添加到 ZSet
                for (RoomPO room : rooms) {
                    double score = room.getStartTime() != null
                            ? room.getStartTime().toEpochSecond(java.time.ZoneOffset.of("+8"))
                            : System.currentTimeMillis() / 1000.0;

                    redisTemplate.opsForZSet().add(cacheKey, room.getId(), score);
                }

                // 设置过期时间
                redisTemplate.expire(cacheKey,
                        RedisKeyConstants.USER_UPCOMING_ROOMS_EXPIRE_TIME,
                        TimeUnit.SECONDS);

                log.debug("[RoomService] 异步缓存即将开始的会议成功 - userId: {}", userId);

            } catch (Exception e) {
                log.error("[RoomService] 异步缓存即将开始的会议失败 - userId: {}", userId, e);
            }
        });
    }

    /**
     * 从缓存构建最近参加的会议列表
     */
    private List<RecentRoomResVO> buildRecentRoomsFromCache(Set<Object> cachedRoomIds, Long userId) {
        List<RecentRoomResVO> resultList = new ArrayList<>();

        for (Object obj : cachedRoomIds) {
            Long roomId = Long.valueOf(obj.toString());

            // 从缓存获取会议信息
            RoomPO room = getRoomFromCache(roomId);
            if (room == null) {
                continue;
            }

            // 从缓存或DB获取参与者信息
            RoomParticipantPO participant = roomParticipantPOMapper
                    .selectByRoomIdAndUserId(roomId, userId);

            if (participant != null) {
                resultList.add(buildRecentRoomVO(room, participant));
            }
        }

        return resultList;
    }

    /**
     * 从数据库构建最近参加的会议列表
     */
    private List<RecentRoomResVO> buildRecentRoomsFromDB(List<RoomParticipantPO> participants) {
        // 批量获取会议信息
        List<Long> roomIds = participants.stream()
                .map(RoomParticipantPO::getRoomId)
                .collect(Collectors.toList());

        List<RoomPO> rooms = roomPOMapper.selectByIds(roomIds);
        Map<Long, RoomPO> roomMap = rooms.stream()
                .collect(Collectors.toMap(RoomPO::getId, r -> r));

        // 批量获取房主信息
        List<Long> hostIds = rooms.stream()
                .map(RoomPO::getHostId)
                .distinct()
                .collect(Collectors.toList());

        List<FindUserByIdRspDTO> hostInfos = userRpcService.findByIds(hostIds);
        Map<Long, FindUserByIdRspDTO> hostInfoMap = hostInfos != null
                ? hostInfos.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, u -> u))
                : new HashMap<>();

        // 构建结果
        return participants.stream()
                .map(p -> {
                    RoomPO room = roomMap.get(p.getRoomId());
                    if (room == null) {
                        return null;
                    }

                    FindUserByIdRspDTO hostInfo = hostInfoMap.get(room.getHostId());
                    return RecentRoomResVO.builder()
                            .roomId(room.getId())
                            .roomNo(room.getRoomNo())
                            .title(room.getTitle())
                            .hostId(room.getHostId())
                            .hostName(hostInfo != null ? hostInfo.getNickName() : "未知")
                            .type(room.getType())
                            .status(room.getStatus())
                            .joinedAt(p.getJoinedAt())
                            .leftAt(p.getLeftAt())
                            .startTime(room.getStartTime())
                            .endTime(room.getEndTime())
                            .role(p.getRole())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    /**
     * 构建单个最近参加的会议VO
     */
    private RecentRoomResVO buildRecentRoomVO(RoomPO room, RoomParticipantPO participant) {
        FindUserByIdRspDTO hostInfo = userRpcService.findById(room.getHostId());

        return RecentRoomResVO.builder()
                .roomId(room.getId())
                .roomNo(room.getRoomNo())
                .title(room.getTitle())
                .hostId(room.getHostId())
                .hostName(hostInfo != null ? hostInfo.getNickName() : "未知")
                .type(room.getType())
                .status(room.getStatus())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .startTime(room.getStartTime())
                .endTime(room.getEndTime())
                .role(participant.getRole())
                .build();
    }

    /**
     * 从缓存构建即将开始的会议列表
     */
    private List<UpcomingRoomResVO> buildUpcomingRoomsFromCache(Set<Object> cachedRoomIds, Long userId) {
        List<UpcomingRoomResVO> resultList = new ArrayList<>();

        for (Object obj : cachedRoomIds) {
            Long roomId = Long.valueOf(obj.toString());
            RoomPO room = getRoomFromCache(roomId);

            if (room != null) {
                resultList.add(buildUpcomingRoomVO(room, userId));
            }
        }

        return resultList;
    }

    /**
     * 从数据库构建即将开始的会议列表
     */
    private List<UpcomingRoomResVO> buildUpcomingRoomsFromDB(List<RoomPO> rooms, Long userId) {
        // 批量获取房主信息
        List<Long> hostIds = rooms.stream()
                .map(RoomPO::getHostId)
                .distinct()
                .collect(Collectors.toList());

        List<FindUserByIdRspDTO> hostInfos = userRpcService.findByIds(hostIds);
        Map<Long, FindUserByIdRspDTO> hostInfoMap = hostInfos != null
                ? hostInfos.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, u -> u))
                : new HashMap<>();

        return rooms.stream()
                .map(room -> {
                    FindUserByIdRspDTO hostInfo = hostInfoMap.get(room.getHostId());
                    return UpcomingRoomResVO.builder()
                            .roomId(room.getId())
                            .roomNo(room.getRoomNo())
                            .title(room.getTitle())
                            .hostId(room.getHostId())
                            .hostName(hostInfo != null ? hostInfo.getNickName() : "未知")
                            .type(room.getType())
                            .maxParticipants(room.getMaxParticipants())
                            .startTime(room.getStartTime())
                            .endTime(room.getEndTime())
                            .createdTime(room.getCreatedTime())
                            .isHost(room.getHostId().equals(userId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建单个即将开始的会议VO
     */
    private UpcomingRoomResVO buildUpcomingRoomVO(RoomPO room, Long userId) {
        FindUserByIdRspDTO hostInfo = userRpcService.findById(room.getHostId());

        return UpcomingRoomResVO.builder()
                .roomId(room.getId())
                .roomNo(room.getRoomNo())
                .title(room.getTitle())
                .hostId(room.getHostId())
                .hostName(hostInfo != null ? hostInfo.getNickName() : "未知")
                .type(room.getType())
                .maxParticipants(room.getMaxParticipants())
                .createdTime(room.getCreatedTime())
                .isHost(room.getHostId().equals(userId))
                .build();
    }

    /**
     * 缓存会议信息（使用 Hash 结构）
     */
    private void cacheRoomInfo(RoomPO room) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, room.getId());

            // 将 RoomPO 转换为 Hash Map
            Map<String, String> hashMap = MapUtils.objectToStringMap(room);

            // 存储到 Redis Hash
            redisTemplate.opsForHash().putAll(roomKey, hashMap);

            // 设置过期时间
            redisTemplate.expire(roomKey,
                    RedisKeyConstants.ROOM_INFO_EXPIRE_TIME,
                    TimeUnit.SECONDS);

            // 缓存roomNo -> roomId映射的hash
            String mappingKey = RedisKeyConstants.ROOM_NO_MAPPING_KEY;
            redisTemplate.opsForHash().put(mappingKey, room.getRoomNo(), room.getId().toString());

            // 延长 Hash 的过期时间（每次写入时刷新）
            redisTemplate.expire(mappingKey,
                    RedisKeyConstants.ROOM_NO_MAPPING_EXPIRE_TIME,
                    TimeUnit.SECONDS);


            log.debug("[RoomService] Cached room info to Hash - roomId: {}, roomNo: {}",
                    room.getId(), room.getRoomNo());

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
            return MapUtils.mapToObject(hashMap,RoomPO.class);

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

    /**
     * 生成唯一的会议号
     * 使用 Redis Set 数据结构存储所有生成的会议号
     */
    private String generateUniqueRoomNo() {
        String roomNoSetKey = RedisKeyConstants.ROOM_NO_SET_KEY;
        int maxRetries = 10;

        for (int i = 0; i < maxRetries; i++) {
            // 生成 1 位随机小写字母
            char randomLetter = (char) (RandomUtil.randomInt(26) + 'A');
            
            // 生成 5 位随机数字
            String randomDigits = RandomUtil.randomNumbers(5);
            
            // 组合成会议号
            String roomNo = randomLetter + randomDigits;

            // 使用 SADD 命令尝试添加到 Set 中
            // 如果 roomNo 已存在，SADD 返回 0；不存在则添加成功，返回 1
            Long addResult = redisTemplate.opsForSet().add(roomNoSetKey, roomNo);

            if (addResult != null && addResult > 0) {
                // 添加成功，说明 roomNo 是唯一的
                log.debug("[RoomService] Generated unique roomNo: {}", roomNo);

                // 为整个 Set 设置过期时间（只在第一次添加时设置）
                Long setSize = redisTemplate.opsForSet().size(roomNoSetKey);
                if (setSize != null && setSize == 1) {
                    // 默认过期时间为 7 天
                    redisTemplate.expire(roomNoSetKey,
                            RedisKeyConstants.ROOM_NO_SET_EXPIRE_TIME,
                            TimeUnit.SECONDS);
                }

                return roomNo;
            }

            log.debug("[RoomService] RoomNo {} already exists, retry {}/{}",
                    roomNo, i + 1, maxRetries);
        }

        // 重试次数用尽，抛出异常
        log.error("[RoomService] Failed to generate unique roomNo after {} retries", maxRetries);
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