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
