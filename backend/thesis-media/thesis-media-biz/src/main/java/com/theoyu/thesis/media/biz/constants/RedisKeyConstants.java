package com.theoyu.thesis.media.biz.constants;

public class RedisKeyConstants {

    // ==================== 房间相关 ====================

    /**
     * 房间信息缓存 Key
     * media:room:{roomId}
     */
    public static final String ROOM_INFO_KEY = "media:room:%s";

    /**
     * 房间配置缓存 Key
     * media:room:config:{roomId}
     */
    public static final String ROOM_CONFIG_KEY = "media:room:config:%s";

    /**
     * 房间自动关闭 Key
     * media:room:auto-close:{roomId}
     */
    public static final String ROOM_AUTO_CLOSE_KEY = "media:room:auto_close:%s";

    /**
     * 房间参与者列表 Key
     * media:room:participants:{roomId}
     */
    public static final String ROOM_PARTICIPANTS_KEY = "media:room:participants:%s";

    /**
     * 参与者详情 Key
     * media:participant:{roomId}:{userId}
     */
    public static final String PARTICIPANT_INFO_KEY = "media:participant:%s:%s";

    /**
     * 媒体统计数据 Key
     * media:stats:{roomId}:{peerId}:{timestamp}
     */
    public static final String MEDIA_STATS_KEY = "media:stats:%s:%s:%s";
    /**
     * 房间消息缓存 ZSet, score为时间戳
     * room:message:{roomId}
     */
    public static final String ROOM_MESSAGE_KEY = "room:message:%s";

    /**
     * 用户房间创建配额 Key
     * media:user:room:quota:{userId}
     */
    public static final String USER_ROOM_QUOTA_KEY = "media:user:room:quota:%s";

    /**
     * 房间号唯一性检查 Key
     * media:room:no:{roomNo}
     */
    public static final String ROOM_NO_SET_KEY = "media:room:no:set";

    /**
     * 会议号映射 Hash Key
     * Redis 数据类型: Hash
     * 用于存储 roomNo -> roomId 的映射关系
     * Hash Field: roomNo, Hash Value: roomId
     */
    public static final String ROOM_NO_MAPPING_KEY = "media:room:no:mapping";


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


    // ==================== SFU 节点相关 ====================

    /**
     * SFU 节点列表 Key
     * media:sfu:nodes
     */
    public static final String SFU_NODES_KEY = "media:sfu:nodes";

    /**
     * SFU 节点详情 Key
     * media:sfu:node:{nodeId}
     */
    public static final String SFU_NODE_INFO_KEY = "media:sfu:node:%s";

    // ==================== 缓存过期时间 ====================

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
     * 参与者信息缓存过期时间：30分钟
     */
    public static final long PARTICIPANT_INFO_EXPIRE_TIME = 30 * 60;

    /**
     * 媒体统计数据过期时间：1天
     */
    public static final long MEDIA_STATS_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * SFU 节点信息过期时间：5分钟
     */
    public static final long SFU_NODE_EXPIRE_TIME = 5 * 60;

}
