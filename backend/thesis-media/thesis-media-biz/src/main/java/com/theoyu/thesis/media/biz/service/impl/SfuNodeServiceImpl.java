package com.theoyu.thesis.media.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.utils.MapUtils;
import com.theoyu.thesis.media.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.media.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import com.theoyu.thesis.media.biz.model.entity.SfuNodePO;
import com.theoyu.thesis.media.biz.model.mapper.RoomPOMapper;
import com.theoyu.thesis.media.biz.model.mapper.SfuNodePOMapper;
import com.theoyu.thesis.media.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.media.biz.service.SfuNodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SfuNodeServiceImpl implements SfuNodeService {

    private static final int NODE_STATUS_ONLINE = 1;

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private SfuNodePOMapper sfuNodePOMapper;

    @Resource
    private RoomPOMapper roomPOMapper;

    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${sfu.discovery.service-name:sfu-server}")
    private String sfuServiceName;

    @Value("${sfu.server.use-ssl:false}")
    private boolean sfuUseSsl;

    @Value("${sfu.server.grpc-port:50052}")
    private int defaultGrpcPort;

    @Override
    public void syncNodesFromNacos() {
        List<ServiceInstance> instances = discoveryClient.getInstances(sfuServiceName);
        if (instances == null || instances.isEmpty()) {
            log.warn("[SfuNodeService] Nacos 未发现 SFU 实例，serviceName={}", sfuServiceName);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (ServiceInstance instance : instances) {
            try {
                String instanceId = resolveInstanceId(instance);
                SfuNodePO existing = sfuNodePOMapper.selectByInstanceId(instanceId);

                if (existing == null) {
                    Long nodeId = Long.valueOf(idGeneratorRpcService.getSfuNodeId());
                    SfuNodePO node = SfuNodePO.builder()
                            .id(nodeId)
                            .instanceId(instanceId)
                            .ipAddress(instance.getHost())
                            .httpPort(instance.getPort())
                            .grpcPort(defaultGrpcPort)
                            .grpcHost(instance.getHost())
                            .region("default")
                            .status(NODE_STATUS_ONLINE)
                            .currentLoad(0)
                            .createdTime(now)
                            .updatedTime(now)
                            .build();
                    sfuNodePOMapper.insert(node);
                    cacheNodeInfo(node);
                    refreshNodeLoadZSet(node);
                    log.info("[SfuNodeService] 新 SFU 节点已入库 - nodeId: {}, instanceId: {}", nodeId, instanceId);
                } else {
                    SfuNodePO update = SfuNodePO.builder()
                            .id(existing.getId())
                            .ipAddress(instance.getHost())
                            .httpPort(instance.getPort())
                            .grpcHost(instance.getHost())
                            .status(NODE_STATUS_ONLINE)
                            .updatedTime(now)
                            .build();
                    sfuNodePOMapper.updateByPrimaryKeySelective(update);
                    
                    // 重新从 DB 查询最新的 currentLoad，而不是保留旧值
                    // 这样可以防止长期运行中负载数据累积不清零的问题
                    SfuNodePO latest = sfuNodePOMapper.selectByPrimaryKey(existing.getId());
                    if (latest != null) {
                        existing = latest;
                        existing.setIpAddress(instance.getHost());
                        existing.setHttpPort(instance.getPort());
                        existing.setGrpcHost(instance.getHost());
                        existing.setStatus(NODE_STATUS_ONLINE);
                    }
                    cacheNodeInfo(existing);
                    refreshNodeLoadZSet(existing);
                    log.debug("[SfuNodeService] SFU 节点已刷新 - nodeId: {}, currentLoad: {}",
                            existing.getId(), existing.getCurrentLoad());
                }
            } catch (Exception e) {
                log.error("[SfuNodeService] 同步 SFU 实例失败 - instance: {}", instance, e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfuNodePO allocateNodeForRoom(Long roomId) {
        RoomPO room = roomPOMapper.selectByPrimaryKey(roomId);
        if (room == null) {
            throw new BusinessException(ResponseCodeEnum.ROOM_NOT_FOUND);
        }

        if (room.getSfuNodeId() != null && room.getSfuNodeId() > 0) {
            SfuNodePO bound = getNodeById(room.getSfuNodeId());
            if (bound != null && Objects.equals(bound.getStatus(), NODE_STATUS_ONLINE)) {
                cacheRoomSfuBinding(roomId, bound.getId());
                return bound;
            }
            log.warn("[SfuNodeService] 房间绑定的 SFU 节点不可用，将重新分配 - roomId: {}, nodeId: {}",
                    roomId, room.getSfuNodeId());
        }

        String lockKey = String.format(RedisKeyConstants.ROOM_SFU_ALLOCATE_LOCK_KEY, roomId);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1",
                RedisKeyConstants.ROOM_SFU_ALLOCATE_LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            waitForPeerAllocation(roomId);
            return resolveBoundNode(roomId);
        }

        try {
            room = roomPOMapper.selectByPrimaryKey(roomId);
            if (room.getSfuNodeId() != null && room.getSfuNodeId() > 0) {
                return getNodeById(room.getSfuNodeId());
            }

            syncNodesFromNacos();
            SfuNodePO selected = pickLeastLoadNode();
            if (selected == null) {
                throw new BusinessException(ResponseCodeEnum.SFU_NODE_UNAVAILABLE);
            }

            int updated = roomPOMapper.updateSfuNodeIdIfAbsent(roomId, selected.getId());
            if (updated == 0) {
                room = roomPOMapper.selectByPrimaryKey(roomId);
                return getNodeById(room.getSfuNodeId());
            }

            // 增加 DB 中的负载计数
            sfuNodePOMapper.incrementCurrentLoad(selected.getId());
            
            // 重新从 DB 查询最新的负载值，确保缓存数据的准确性
            // 这样可以避免多实例环境下因内存数据不同步导致的缓存不一致问题
            SfuNodePO refreshed = sfuNodePOMapper.selectByPrimaryKey(selected.getId());
            if (refreshed != null) {
                selected = refreshed;
            }
            
            cacheNodeInfo(selected);
            refreshNodeLoadZSet(selected);
            cacheRoomSfuBinding(roomId, selected.getId());
            updateRoomSfuInRedis(roomId, selected.getId());

            log.info("[SfuNodeService] 房间 SFU 节点分配成功 - roomId: {}, nodeId: {}, instanceId: {}",
                    roomId, selected.getId(), selected.getInstanceId());
            return selected;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public SfuNodePO getNodeById(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        try {
            String nodeKey = String.format(RedisKeyConstants.SFU_NODE_INFO_KEY, nodeId);
            Map<Object, Object> hashMap = redisTemplate.opsForHash().entries(nodeKey);
            if (!hashMap.isEmpty()) {
                return MapUtils.mapToObject(hashMap, SfuNodePO.class);
            }
        } catch (Exception e) {
            log.warn("[SfuNodeService] 从 Redis 读取节点失败 - nodeId: {}", nodeId, e);
        }

        SfuNodePO node = sfuNodePOMapper.selectByPrimaryKey(nodeId);
        if (node != null) {
            cacheNodeInfo(node);
            refreshNodeLoadZSet(node);
        }
        return node;
    }

    @Override
    public String buildSfuServerUrl(SfuNodePO node) {
        if (node == null) {
            return "";
        }
        String scheme = sfuUseSsl ? "wss" : "ws";
        int port = node.getHttpPort() != null ? node.getHttpPort() : 3000;
        String host = node.getIpAddress();
        return scheme + "://" + host + ":" + port;
    }

    /**
     * 递减 SFU 节点的负载（房间关闭时调用）
     * 
     * @param sfuNodeId SFU 节点 ID
     * @return 递减后的负载值
     */
    @Override
    public Integer decrementNodeLoad(Long sfuNodeId) {
        if (sfuNodeId == null) {
            log.warn("[SfuNodeService] decrementNodeLoad - sfuNodeId is null");
            return 0;
        }

        try {
            // 1. 从 DB 递减负载
            sfuNodePOMapper.decrementCurrentLoad(sfuNodeId);

            // 2. 重新查询最新的负载值
            SfuNodePO node = sfuNodePOMapper.selectByPrimaryKey(sfuNodeId);
            if (node != null) {
                // 3. 更新缓存
                cacheNodeInfo(node);
                refreshNodeLoadZSet(node);

                log.info("[SfuNodeService] 节点负载已递减 - nodeId: {}, newLoad: {}",
                        sfuNodeId, node.getCurrentLoad());
                return node.getCurrentLoad() != null ? node.getCurrentLoad() : 0;
            } else {
                log.warn("[SfuNodeService] 节点不存在或已被删除 - nodeId: {}", sfuNodeId);
                return 0;
            }
        } catch (Exception e) {
            log.error("[SfuNodeService] 递减节点负载失败 - nodeId: {}", sfuNodeId, e);
            return 0;
        }
    }

    private SfuNodePO pickLeastLoadNode() {
        try {
            var members = redisTemplate.opsForZSet().range(RedisKeyConstants.SFU_NODE_LOAD_ZSET_KEY, 0, 0);
            if (members != null && !members.isEmpty()) {
                Long nodeId = Long.valueOf(members.iterator().next().toString());
                SfuNodePO cached = getNodeById(nodeId);
                if (cached != null && Objects.equals(cached.getStatus(), NODE_STATUS_ONLINE)) {
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("[SfuNodeService] 从 ZSet 选取节点失败，降级为数据库查询", e);
        }

        // 降级查询数据库获取最小负载节点
        List<SfuNodePO> nodes = sfuNodePOMapper.selectAvailableNodes();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        
        SfuNodePO selected = nodes.stream()
                .min(Comparator.comparingInt(n -> n.getCurrentLoad() == null ? 0 : n.getCurrentLoad()))
                .orElse(null);
        
        // 将从 DB 查询的结果缓存到 Redis，避免下次继续查询 DB
        try {
            cacheNodeInfo(selected);
            refreshNodeLoadZSet(selected);
            log.debug("[SfuNodeService] 已从 DB 查询并缓存负载最低节点 - nodeId: {}, currentLoad: {}",
                    selected.getId(), selected.getCurrentLoad());
        } catch (Exception e) {
            log.warn("[SfuNodeService] 缓存 DB 查询结果失败", e);
        }

        return selected;
    }

    private SfuNodePO resolveBoundNode(Long roomId) {
        Object cachedNodeId = redisTemplate.opsForValue().get(String.format(RedisKeyConstants.ROOM_SFU_NODE_KEY, roomId));
        if (cachedNodeId != null) {
            return getNodeById(Long.valueOf(cachedNodeId.toString()));
        }
        RoomPO room = roomPOMapper.selectByPrimaryKey(roomId);
        if (room != null && room.getSfuNodeId() != null && room.getSfuNodeId() > 0) {
            return getNodeById(room.getSfuNodeId());
        }
        throw new BusinessException(ResponseCodeEnum.SFU_NODE_ALLOCATION_FAILED);
    }

    private void waitForPeerAllocation(Long roomId) {
        for (int i = 0; i < 10; i++) {
            RoomPO room = roomPOMapper.selectByPrimaryKey(roomId);
            if (room != null && room.getSfuNodeId() != null && room.getSfuNodeId() > 0) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private String resolveInstanceId(ServiceInstance instance) {
        if (instance.getInstanceId() != null && !instance.getInstanceId().isBlank()) {
            return instance.getInstanceId();
        }
        return instance.getHost() + ":" + instance.getPort();
    }

    private void cacheNodeInfo(SfuNodePO node) {
        try {
            String nodeKey = String.format(RedisKeyConstants.SFU_NODE_INFO_KEY, node.getId());
            Map<String, String> hashMap = MapUtils.objectToStringMap(node);
            redisTemplate.opsForHash().putAll(nodeKey, hashMap);
            redisTemplate.expire(nodeKey, RedisKeyConstants.SFU_NODE_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[SfuNodeService] 缓存节点信息失败 - nodeId: {}", node.getId(), e);
        }
    }

    private void refreshNodeLoadZSet(SfuNodePO node) {
        try {
            double score = node.getCurrentLoad() == null ? 0 : node.getCurrentLoad();
            redisTemplate.opsForZSet().add(RedisKeyConstants.SFU_NODE_LOAD_ZSET_KEY,
                    node.getId().toString(), score);
        } catch (Exception e) {
            log.warn("[SfuNodeService] 刷新节点负载 ZSet 失败 - nodeId: {}", node.getId(), e);
        }
    }

    private void cacheRoomSfuBinding(Long roomId, Long nodeId) {
        String key = String.format(RedisKeyConstants.ROOM_SFU_NODE_KEY, roomId);
        redisTemplate.opsForValue().set(key, nodeId,
                RedisKeyConstants.ROOM_SFU_NODE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private void updateRoomSfuInRedis(Long roomId, Long sfuNodeId) {
        try {
            String roomKey = String.format(RedisKeyConstants.ROOM_INFO_KEY, roomId);
            redisTemplate.opsForHash().put(roomKey, "sfuNodeId", String.valueOf(sfuNodeId));
        } catch (Exception e) {
            log.warn("[SfuNodeService] 更新房间缓存 sfuNodeId 失败 - roomId: {}", roomId, e);
        }
    }
}
