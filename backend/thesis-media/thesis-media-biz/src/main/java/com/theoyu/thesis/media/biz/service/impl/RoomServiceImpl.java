package com.theoyu.thesis.media.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.PageResponse;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.framework.common.utils.MapUtils;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.grpc.SFUGrpcServer;
import com.theoyu.thesis.media.biz.model.dto.RoomCreatedEventDTO;
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
                // 构建消息事件对象
                RoomCreatedEventDTO event = RoomCreatedEventDTO.builder()
                        .roomId(roomId)
                        .roomNo(roomNo)
                        .hostId(userId)
                        .title(reqVO.getTitle())
                        .timestamp(now)
                        .totalParticipants(0)
                        .totalMessages(0)
                        .duration(0)
                        .build();

                // 直接序列化 DTO 对象
                rocketMQTemplate.convertAndSend(
                        MQConstants.TOPIC_MEDIA_ROOM_EVENT + ":" + MQConstants.TAG_ROOM_CREATED,
                        JsonUtils.toJsonString(event)
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

        // 1. 判断入参是 roomId 还是 roomNo
        Long roomId = parseRoomId(roomIdOrNo);

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
        List<ParticipantListItemVO> participants = getOnlineParticipants(roomId);

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

    @Override
    public GetRoomDetailResVO getRoomDetail(String roomIdOrNo) {
        log.info("[RoomService] 获取会议详情 - roomIdOrNo: {}", roomIdOrNo);

        try {
            // 1. 解析 roomId
            Long roomId = parseRoomId(roomIdOrNo);

            // 2. 获取会议基础信息
            RoomPO room = getRoomFromCache(roomId);
            if (room == null) {
                throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
            }

            // 3. 获取主持人信息(需要优化)
            FindUserByIdRspDTO hostInfo = userRpcService.findById(room.getHostId());

            // 4. 获取全部参与者列表（优先从缓存）
            List<ParticipantListItemVO> participants =
                    getAllParticipantsForDetail(roomId);

            // 5. 计算持续时间（分钟）
            Integer duration = calculateDuration(room);

            // 6. 构建响应
            return GetRoomDetailResVO.builder()
                    .roomId(room.getId())
                    .roomNo(room.getRoomNo())
                    .title(room.getTitle())
                    .description(room.getSettings()) // 假设 settings 包含描述信息
                    .startTime(room.getStartTime())
                    .duration(duration)
                    .status(room.getStatus())
                    .host(hostInfo)
                    .participantCount(participants.size())
                    .participants(participants)
                    .recording(buildRecordingInfo())
                    .transcript(buildTranscriptInfo())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RoomService] 获取会议详情失败 - roomIdOrNo: {}", roomIdOrNo, e);
            throw new BusinessException(ResponseCodeEnum.ROOM_DETAIL_QUERY_FAILED);
        }
    }

    /**
     * 加入会议（预验证阶段）
     * 此方法不会执行持久化操作。真正的参与者记录将在用户成功连接到 SFU 服务器后，
     * 由 SFU 通过 gRPC 调用 {@link SFUGrpcServer#notifyParticipantJoined} 时创建。
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

            if (cacheSize != null && cacheSize > 0) {
                // 缓存命中：使用 ZSet 的范围查询
                long start = PageResponse.getOffset(page, size);
                long end = start + size - 1;

                // 按分数倒序获取（最近的在前）
                Set<Object> cachedRoomIds = redisTemplate.opsForZSet()
                        .reverseRange(cacheKey, start, end);

                if (cachedRoomIds != null && !cachedRoomIds.isEmpty()) {
                    resultList = buildRecentRoomsFromCache(cachedRoomIds, userId);
                } else {
                    resultList = Collections.emptyList();
                }
            } else {
                // 缓存未命中：查询数据库
                Long offset = PageResponse.getOffset(page, size);
                List<RoomParticipantPO> records = roomParticipantPOMapper
                        .selectRecentRoomsByUserId(userId, offset, size);


                if (!records.isEmpty()) {
                    resultList = buildRecentRoomsFromDB(records);

                    // 异步回写缓存
                    asyncCacheRecentRooms(userId, records);
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
                    log.info("[RoomService] 缓存命中 - userId: {}, count: {}", userId, totalCount);
                } else {
                    resultList = Collections.emptyList();
                    totalCount = 0;
                }
            } else {
                // 缓存未命中：查询数据库
                log.info("[RoomService] 缓存未命中，查询数据库 - userId: {}", userId);

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

    @Override
    public PageResponse<ParticipantListItemVO> getParticipants(GetParticipantsReqVO reqVO) {
        Long roomId = reqVO.getRoomId();
        long page = reqVO.getPage() > 0 ? reqVO.getPage() : 1;
        long size = reqVO.getSize() > 0 ? reqVO.getSize() : 20;
        Integer status = reqVO.getStatus();

        try {
            // 1. 检查房间是否存在
            RoomPO room = getRoomFromCache(roomId);
            if (room == null) {
                throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
            }

            // 2. 尝试从 Set 缓存获取在线参与者列表
            if (status == null || status == 1) {
                List<ParticipantListItemVO> cachedList = getOnlineParticipantsFromCache(roomId, page, size);
                if (!cachedList.isEmpty()) {
                    log.info("[RoomService] Set 缓存命中在线参与者列表 - roomId: {}", roomId);
                    return PageResponse.success(cachedList, page, size);
                }
            }

            // 3. 缓存未命中,查询数据库
            long offset = PageResponse.getOffset(page, size);
            List<RoomParticipantPO> participants;

            if (status != null) {
                participants = roomParticipantPOMapper.selectByRoomIdAndStatusWithPage(
                        roomId, status, offset, size);
            } else {
                participants = roomParticipantPOMapper.selectByRoomIdWithPage(
                        roomId, offset, size);
            }

            if (participants.isEmpty()) {
                return PageResponse.success(Collections.emptyList(), page, size);
            }

            // 4. 批量查询用户信息
            List<Long> userIds = participants.stream()
                    .map(RoomParticipantPO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            List<FindUserByIdRspDTO> userInfos = userRpcService.findByIds(userIds);
            Map<Long, FindUserByIdRspDTO> userInfoMap = userInfos != null
                    ? userInfos.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, u -> u))
                    : new HashMap<>();

            // 5. 构建返回结果
            List<ParticipantListItemVO> resultList = participants.stream()
                    .map(p -> buildParticipantVO(p, userInfoMap.get(p.getUserId())))
                    .collect(Collectors.toList());

            // 6. 异步缓存在线参与者列表到 Set (仅状态为1时)
            if (status == null || status == 1) {
                asyncCacheOnlineParticipants(roomId, participants, userInfoMap);
            }

            return PageResponse.success(resultList, page, size);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RoomService] 获取房间参与者列表失败 - roomId: {}", roomId, e);
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }
    /**
     * 添加参与者到缓存（同时更新在线和全部参与者列表）
     *
     * @param roomId 房间ID
     * @param participant 参与者PO对象
     */
    @Override
    public void addParticipantToCache(Long roomId, RoomParticipantPO participant) {
        try {
            Long userId = participant.getUserId();

            // 1. 批量查询用户信息
            FindUserByIdRspDTO userInfo = userRpcService.findById(userId);

            // 2. 构建 VO 对象
            ParticipantListItemVO vo = buildParticipantVO(participant, userInfo);

            // 3. 序列化为 JSON
            String participantJson = JsonUtils.toJsonString(vo);

            if (participantJson == null || participantJson.isEmpty()) {
                log.warn("[RoomService] 参与者 JSON 序列化失败 - userId: {}", userId);
                return;
            }

            // 4. 添加到在线参与者 Set
            String onlineKey = String.format(
                    RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);
            redisTemplate.opsForSet().add(onlineKey, participantJson);
            redisTemplate.expire(onlineKey,
                    RedisKeyConstants.PARTICIPANT_LIST_EXPIRE_TIME,
                    TimeUnit.SECONDS);

            // 5. 添加到全部参与者 Hash
            String allParticipantsKey = String.format(
                    RedisKeyConstants.ROOM_ALL_PARTICIPANTS_KEY, roomId);
            redisTemplate.opsForHash().put(
                    allParticipantsKey,
                    userId.toString(),
                    participantJson
            );

            redisTemplate.expire(allParticipantsKey,
                    RedisKeyConstants.ROOM_ALL_PARTICIPANTS_EXPIRE_TIME,
                    TimeUnit.SECONDS);

            // 6. 同步更新用户最近参加会议的 ZSet 缓存
            updateUserRecentRoomsCache(userId, roomId, participant.getJoinedAt());

            log.info("[RoomService] 参与者已添加到缓存 - roomId: {}, userId: {}",
                    roomId, userId);

        } catch (Exception e) {
            log.error("[RoomService] 添加参与者到缓存失败 - roomId: {}, userId: {}",
                    roomId, participant.getUserId(), e);
        }
    }
    /**
     * 从在线参与者缓存中移除（保留全部参与者列表）
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     */
    @Override
    public void removeOnlineParticipantFromCache(Long roomId, Long userId) {
        try {
            String onlineKey = String.format(
                    RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);

            // 获取所有在线成员
            Set<Object> members = redisTemplate.opsForSet().members(onlineKey);

            if (members != null) {
                for (Object member : members) {
                    ParticipantListItemVO vo = JsonUtils.parseObject(
                            member.toString(), ParticipantListItemVO.class);

                    if (vo != null && vo.getUserId().equals(userId)) {
                        // 从在线 Set 中删除
                        redisTemplate.opsForSet().remove(onlineKey, member);

                        log.info("[RoomService] 从在线缓存移除参与者 - roomId: {}, userId: {}",
                                roomId, userId);
                        break;
                    }
                }
            }

            // 检查在线 Set 是否为空
            Long size = redisTemplate.opsForSet().size(onlineKey);
            if (size != null && size == 0) {
                redisTemplate.delete(onlineKey);
                log.info("[RoomService] 在线 Set 为空已删除 - roomId: {}", roomId);
            }

            // 不删除 room:all-participants:{roomId}，保留历史参与记录

        } catch (Exception e) {
            log.error("[RoomService] 从在线缓存移除参与者失败 - roomId: {}, userId: {}",
                    roomId, userId, e);
        }
    }



    // ==================== 私有辅助方法 ====================

    /**
     * 实时更新用户最近参加会议缓存（ZSet）
     * 在参与者加入会议时同步调用，确保缓存与 DB 实时一致
     */
    private void updateUserRecentRoomsCache(Long userId, Long roomId, LocalDateTime joinedAt) {
        try {
            String cacheKey = String.format(RedisKeyConstants.USER_RECENT_ROOMS_KEY, userId);

            double score = joinedAt != null
                    ? joinedAt.toEpochSecond(java.time.ZoneOffset.of("+8"))
                    : System.currentTimeMillis() / 1000.0;

            // 使用 ZADD，若 roomId 已存在则更新 score 为最新加入时间
            redisTemplate.opsForZSet().add(cacheKey, roomId, score);

            // 设置/刷新过期时间
            redisTemplate.expire(cacheKey, RedisKeyConstants.USER_RECENT_ROOMS_EXPIRE_TIME, TimeUnit.SECONDS);

            // 保留最新的 100 条，移除最早的
            Long size = redisTemplate.opsForZSet().size(cacheKey);
            if (size != null && size > 100) {
                redisTemplate.opsForZSet().removeRange(cacheKey, 0, size - 101);
            }

            log.info("[RoomService] 用户最近会议缓存已更新 - userId: {}, roomId: {}", userId, roomId);

        } catch (Exception e) {
            log.error("[RoomService] 更新用户最近会议缓存失败 - userId: {}, roomId: {}", userId, roomId, e);
            // 缓存更新失败不影响主流程
        }
    }

    /**
     * 解析 roomId（兼容 roomId 和 roomNo）
     */
    private Long parseRoomId(String roomIdOrNo) {
        try {
            // 尝试直接解析为 Long
            return Long.parseLong(roomIdOrNo);
        } catch (NumberFormatException e) {
            // 从 Hash 缓存获取映射
            String mappingKey = RedisKeyConstants.ROOM_NO_MAPPING_KEY;
            Object cachedRoomId = redisTemplate.opsForHash().get(mappingKey, roomIdOrNo);

            if (cachedRoomId != null) {
                return Long.parseLong(cachedRoomId.toString());
            }

            // 缓存未命中，查询 DB
            RoomPO room = roomPOMapper.selectByRoomNo(roomIdOrNo);
            if (room == null) {
                throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
            }

            // 回写缓存
            redisTemplate.opsForHash().put(mappingKey, roomIdOrNo, room.getId().toString());
            // 设置 Hash 的过期时间
            redisTemplate.expire(mappingKey,
                    RedisKeyConstants.ROOM_NO_MAPPING_EXPIRE_TIME,
                    TimeUnit.SECONDS);


            return room.getId();
        }
    }



    /**
     * 获取会议详情的全部参与者列表（复用 getParticipants 逻辑）
     * 与 getParticipants 的区别：
     * 1. 不分页，返回全量数据
     * 2. 优先从 Set 缓存获取
     */
    private List<ParticipantListItemVO> getAllParticipantsForDetail(Long roomId) {
        try {
            // 1. 尝试从 Set 缓存获取全部参与者
            String cacheKey = String.format(
                    RedisKeyConstants.ROOM_ALL_PARTICIPANTS_KEY, roomId);
            List<Object> values = redisTemplate.opsForHash().values(cacheKey);

            if (!values.isEmpty()) {
                log.info("[RoomService] 缓存命中全部参与者 - roomId: {}, count: {}",
                        roomId, values.size());

                return values.stream()
                        .map(obj -> JsonUtils.parseObject(obj.toString(),
                                ParticipantListItemVO.class))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(p ->
                                p.getJoinedAt() != null ? p.getJoinedAt() : LocalDateTime.MIN))
                        .collect(Collectors.toList());
            }

            // 2. 缓存未命中，查询 DB（复用 getParticipants 的查询逻辑）
            log.info("[RoomService] 缓存未命中，从 DB 查询全部参与者 - roomId: {}", roomId);

            List<RoomParticipantPO> participants =
                    roomParticipantPOMapper.selectByRoomIdAndStatus(roomId, null);

            if (participants.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 批量查询用户信息（复用逻辑）
            List<Long> userIds = participants.stream()
                    .map(RoomParticipantPO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            List<FindUserByIdRspDTO> userInfos = userRpcService.findByIds(userIds);
            Map<Long, FindUserByIdRspDTO> userInfoMap = userInfos != null
                    ? userInfos.stream().collect(Collectors.toMap(
                    FindUserByIdRspDTO::getId, u -> u))
                    : new HashMap<>();

            // 4. 构建 VO 列表（复用 buildParticipantVO 方法）
            List<ParticipantListItemVO> resultList = participants.stream()
                    .map(p -> buildParticipantVO(p, userInfoMap.get(p.getUserId())))
                    .collect(Collectors.toList());

            // 5. 异步缓存到 Set（与在线参与者使用不同的 Key）
            asyncCacheAllParticipantsToHash(roomId, resultList);

            return resultList;

        } catch (Exception e) {
            log.error("[RoomService] 获取全部参与者失败 - roomId: {}", roomId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 异步缓存全部参与者到 Set（独立 Key）
     */
    private void asyncCacheAllParticipantsToHash(Long roomId,
                                                List<ParticipantListItemVO> participants) {

        threadPoolTaskExecutor.execute(() -> {
            try {
                String cacheKey = String.format(
                        RedisKeyConstants.ROOM_ALL_PARTICIPANTS_KEY, roomId);

                // 删除旧缓存
                redisTemplate.delete(cacheKey);

                // 批量添加到 Set
                Map<String, String> hashMap = new HashMap<>();
                for (ParticipantListItemVO p : participants) {
                    String json = JsonUtils.toJsonString(p);
                    if (json != null && !json.isEmpty()) {
                        hashMap.put(p.getUserId().toString(), json);
                    }
                }
                if (!hashMap.isEmpty()) {
                    redisTemplate.opsForHash().putAll(cacheKey, hashMap);
                }

                // 设置过期时间
                redisTemplate.expire(cacheKey,
                        RedisKeyConstants.ROOM_ALL_PARTICIPANTS_EXPIRE_TIME,
                        TimeUnit.SECONDS);

                log.info("[RoomService] 异步缓存全部参与者成功 - roomId: {}, count: {}",
                        roomId, participants.size());

            } catch (Exception e) {
                log.error("[RoomService] 异步缓存全部参与者失败 - roomId: {}", roomId, e);
            }
        });
    }

    /**
     * 计算会议持续时间（分钟）
     */
    private Integer calculateDuration(RoomPO room) {
        if (room.getEndTime() == null) {
            return 0;
        }

        LocalDateTime start = room.getStartTime();
        LocalDateTime end = room.getEndTime();

        if (start == null) {
            return 0;
        }

        return (int) java.time.Duration.between(start, end).toMinutes();
    }

    /**
     * 构建录像信息（预留）
     */
    private GetRoomDetailResVO.RecordingInfo buildRecordingInfo() {
        return GetRoomDetailResVO.RecordingInfo.builder()
                .available(false)
                .url("")
                .size(0L)
                .duration(0)
                .build();
    }

    /**
     * 构建记录信息（预留）
     */
    private GetRoomDetailResVO.TranscriptInfo buildTranscriptInfo() {
        return GetRoomDetailResVO.TranscriptInfo.builder()
                .available(false)
                .url("")
                .build();
    }

    /**
     * 从 Set 缓存获取在线参与者列表 (带分页和排序)
     */
    private List<ParticipantListItemVO> getOnlineParticipantsFromCache(Long roomId, long page, long size) {
        try {
            String cacheKey = String.format(RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);

            // 获取 Set 中的所有成员
            Set<Object> members = redisTemplate.opsForSet().members(cacheKey);

            if (members == null || members.isEmpty()) {
                return Collections.emptyList();
            }

            // 反序列化为 VO 对象
            List<ParticipantListItemVO> allParticipants = members.stream()
                    .map(obj -> JsonUtils.parseObject(obj.toString(), ParticipantListItemVO.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 按加入时间排序(升序,先加入的在前)
            allParticipants.sort((a, b) -> {
                if (a.getJoinedAt() == null) return 1;
                if (b.getJoinedAt() == null) return -1;
                return a.getJoinedAt().compareTo(b.getJoinedAt());
            });

            // 手动分页
            long start = PageResponse.getOffset(page, size);
            long end = Math.min(start + size, allParticipants.size());

            if (start >= allParticipants.size()) {
                return Collections.emptyList();
            }

            return allParticipants.subList((int) start, (int) end);

        } catch (Exception e) {
            log.warn("[RoomService] 从 Set 缓存获取在线参与者失败 - roomId: {}", roomId, e);
            return Collections.emptyList();
        }
    }
    /**
     * 异步缓存在线参与者列表到 Set (简化版)
     */
    private void asyncCacheOnlineParticipants(Long roomId,
                                              List<RoomParticipantPO> participants,
                                              Map<Long, FindUserByIdRspDTO> userInfoMap) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                // 1. 过滤在线参与者
                // 2. 构建缓存 Key
                String cacheKey = String.format(RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);

                // 3. 删除旧数据
                redisTemplate.delete(cacheKey);

                // 4. 批量添加新成员
                for (RoomParticipantPO p : participants) {
                    ParticipantListItemVO vo = buildParticipantVO(p, userInfoMap.get(p.getUserId()));
                    String json = JsonUtils.toJsonString(vo);

                    if (json != null && !json.isEmpty()) {
                        redisTemplate.opsForSet().add(cacheKey, json);
                    }
                }

                redisTemplate.expire(cacheKey,
                            RedisKeyConstants.PARTICIPANT_LIST_EXPIRE_TIME,
                            TimeUnit.SECONDS);

                log.info("[RoomService] 批量缓存成功 - roomId: {}", roomId);

            } catch (Exception e) {
                log.error("[RoomService] 异步缓存在线参与者列表失败 - roomId: {}", roomId, e);
            }
        });
    }
    /**
     * 构建参与者 VO 对象
     */
    private ParticipantListItemVO buildParticipantVO(RoomParticipantPO participant,
                                                     FindUserByIdRspDTO userInfo) {
        return ParticipantListItemVO.builder()
                .userId(participant.getUserId())
                .userName(userInfo != null ? userInfo.getNickName() : "未知用户")
                .avatar(userInfo != null ? userInfo.getAvatar() : "")
                .role(participant.getRole())
                .status(participant.getStatus())
                .audioMuted(participant.getAudioMuted())
                .videoMuted(participant.getVideoMuted())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .build();
    }
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

                log.info("[RoomService] 异步缓存最近参加的会议成功 - userId: {}", userId);

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

                log.info("[RoomService] 异步缓存即将开始的会议成功 - userId: {}", userId);

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


            log.info("[RoomService] Cached room info to Hash - roomId: {}, roomNo: {}",
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
                log.info("[RoomService] Cache miss, query from DB - roomId: {}", roomId);
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

            log.info("[RoomService] Updated room status in cache - roomId: {}, status: {}",
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
                log.info("[RoomService] Generated unique roomNo: {}", roomNo);

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

            log.info("[RoomService] RoomNo {} already exists, retry {}/{}",
                    roomNo, i + 1, maxRetries);
        }

        // 重试次数用尽，抛出异常
        log.error("[RoomService] Failed to generate unique roomNo after {} retries", maxRetries);
        throw new BusinessException(ResponseCodeEnum.ROOM_NO_DUPLICATE);
    }

    private Integer getCurrentParticipantCount(Long roomId) {
        String participantsKey = String.format(RedisKeyConstants.ROOM_ONLINE_PARTICIPANTS_KEY, roomId);
        Long redisCount = redisTemplate.opsForSet().size(participantsKey);

        if (redisCount == null) {
            return roomParticipantPOMapper.countByRoomIdAndStatus(roomId, 1);
        }

        return Math.toIntExact(redisCount);
    }

    private List<ParticipantListItemVO> getOnlineParticipants(Long roomId) {
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
            return ParticipantListItemVO.builder()
                    .userId(p.getUserId())
                    .userName(userInfo != null ? userInfo.getNickName() : "未知用户")
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