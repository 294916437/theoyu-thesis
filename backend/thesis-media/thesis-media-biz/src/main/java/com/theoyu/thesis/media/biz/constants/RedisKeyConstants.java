package com.theoyu.thesis.media.biz.constants;

public class RedisKeyConstants {

    // ==================== 房间相关 ====================

    /**
     * 房间信息缓存 Key
     * room:{roomId}
     */
    public static final String ROOM_INFO_KEY = "room:%s";

    /**
     * 房间配置缓存 Key
     * room:config:{roomId}
     */
    public static final String ROOM_CONFIG_KEY = "room:config:%s";

    /**
     * 房间自动关闭 Key
     */
    public static final String ROOM_AUTO_CLOSE_KEY = "room:auto_close:%s";

    /**
     * 房间在线参与者列表 Key
     * room:participants:{roomId}
     */
    public static final String ROOM_ONLINE_PARTICIPANTS_KEY = "room:online-participants:%s";
    /**
     * 会议所有参与者列表（Set）
     * room:all-participants:{roomId}
     */
    public static final String ROOM_ALL_PARTICIPANTS_KEY = "room:all-participants:%s";

    /**
     * 参与者详情 Key
     * media:participant:{roomId}:{userId}
     */
    public static final String PARTICIPANT_INFO_KEY = "participant:%s:%s";

    /**
     * 媒体统计数据 Key
     * media:stats:{roomId}:{peerId}:{timestamp}
     */
    public static final String MEDIA_STATS_KEY = "room:stats:%s:%s:%s";
    /**
     * 房间消息缓存 ZSet, score为时间戳
     * room:message:{roomId}
     */
    public static final String ROOM_MESSAGE_KEY = "room:message:%s";

    /**
     * 用户房间创建配额 Key
     * media:user:room:quota:{userId}
     */
    public static final String USER_ROOM_QUOTA_KEY = "user:room:quota:%s";

    /**
     * 房间号唯一性检查 Key
     * media:room:no:{roomNo}
     */
    public static final String ROOM_NO_SET_KEY = "room:no:set";

    /**
     * 会议号映射 Hash Key
     * Redis 数据类型: Hash
     * 用于存储 roomNo -> roomId 的映射关系
     * Hash Field: roomNo, Hash Value: roomId
     */
    public static final String ROOM_NO_MAPPING_KEY = "room:no:mapping";


    /**
     * 用户最近参加的会议列表（Sorted Set，按参与时间排序）
     * user:recent:rooms:{userId}
     */
    public static final String USER_RECENT_ROOMS_KEY = "user:recent:rooms:%d";
    /**
     * 用户即将开始的会议列表（Sorted Set，按计划开始时间排序）
     * user:upcoming:rooms:{userId}
     */
    public static final String USER_UPCOMING_ROOMS_KEY = "user:upcoming:rooms:%d";

    /**
     * 录制状态 Hash
     * 存储: hostId, status, startTime, format
     * Key: room:recording:status:{roomId}
     */
    public static final String ROOM_RECORDING_STATUS_KEY = "room:recording:%s:%s";





    // ==================== SFU 节点相关 ====================


    /**
     * SFU 节点列表 Key
     * media:sfu:nodes
     */
    public static final String SFU_NODES_KEY = "sfu:nodes";

    /**
     * SFU 节点详情 Key
     * media:sfu:node:{nodeId}
     */
    public static final String SFU_NODE_INFO_KEY = "sfu:node:%s";

    /**
     * 房间已绑定的 SFU 节点 ID
     * room:sfu:node:{roomId}
     */
    public static final String ROOM_SFU_NODE_KEY = "room:sfu:node:%s";

    /**
     * 房间 SFU 服务器 URL（直连地址，用于前端非代理场景）
     * room:sfu:url:{roomId}
     */
    public static final String ROOM_SFU_URL_KEY = "room:sfu:url:%s";

    /**
     * 房间 SFU 分配分布式锁
     * room:sfu:allocate:lock:{roomId}
     */
    public static final String ROOM_SFU_ALLOCATE_LOCK_KEY = "room:sfu:allocate:lock:%s";

    /**
     * SFU 节点负载排序 ZSet（score=currentLoad，member=nodeId）
     */
    public static final String SFU_NODE_LOAD_ZSET_KEY = "sfu:nodes:load";

    // ==================== 缓存过期时间 ====================
    /**
     * 房间所有参与者
     */
    public static final long ROOM_ALL_PARTICIPANTS_EXPIRE_TIME = 30 * 60;
    /**
     * 会议号映射过期时间（7天）
     */
    public static final Long ROOM_NO_MAPPING_EXPIRE_TIME = 7 * 24 * 60 * 60L;
    public static final long USER_RECENT_ROOMS_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7天


    public static final long USER_UPCOMING_ROOMS_EXPIRE_TIME = 24 * 60 * 60; // 1天

    /**
     * 用户房间创建配额过期时间：1小时（用于限流）
     */
    public static final long USER_ROOM_QUOTA_EXPIRE_TIME = 60 * 60;

    /**
     * 会议号集合过期时间（7天）
     */
    public static final Long ROOM_NO_SET_EXPIRE_TIME = 7 * 24 * 60 * 60L;

    /**
     * 房间号缓存过期时间：永久（直到房间删除）
     */
    public static final long ROOM_NO_EXPIRE_TIME = -1;

    /**
     * 房间信息缓存过期时间：30分钟
     */
    public static final long ROOM_INFO_EXPIRE_TIME = 30 * 60;

    /**
     * 房间配置缓存过期时间：1小时
     */
    public static final long ROOM_CONFIG_EXPIRE_TIME = 60 * 60;

    /**
     * 参与者缓存过期时间：30分钟
     */
    public static final long PARTICIPANT_INFO_EXPIRE_TIME = 30 * 60;
    /**
     * 参与者列表缓存过期时间：30分钟
     */
    public static final long PARTICIPANT_LIST_EXPIRE_TIME = 10 * 60;

    /**
     * 媒体统计数据过期时间：1天
     */
    public static final long MEDIA_STATS_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * SFU 节点信息过期时间：5分钟
     */
    public static final long SFU_NODE_EXPIRE_TIME = 5 * 60;

    /**
     * 房间 SFU 绑定缓存过期时间：与房间信息一致
     */
    public static final long ROOM_SFU_NODE_EXPIRE_TIME = 30 * 60;

    /**
     * SFU 分配锁过期时间（秒）
     */
    public static final long ROOM_SFU_ALLOCATE_LOCK_EXPIRE_TIME = 15;
    /**
     * 录制状态缓存过期时间：3*24小时
     */
    public static final long ROOM_RECORDING_STATUS_EXPIRE_TIME = 3*24 * 60 * 60L;

    // ==================== 预约会议自动激活 ====================

    /**
     * 预约会议自动激活延迟队列（ZSet，score 为激活时制01秒时间戳）
     * room:activation:queue
     */
    public static final String ROOM_ACTIVATION_QUEUE_KEY = "room:activation:queue";

    /**
     * 预约会议激活调度器分布式锁，避免多实例重复扫描同一批任务。
     */
    public static final String ROOM_ACTIVATION_SCHEDULER_LOCK_KEY = "room:activation:scheduler:lock";

}
