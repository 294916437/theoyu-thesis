package com.theoyu.thesis.media.biz.constants;

public interface MQConstants {
    // ==================== Topic ====================

    /**
     * 媒体房间事件 Topic
     */
    public static final String TOPIC_MEDIA_ROOM_EVENT = "media-room-event";

    /**
     * 媒体统计数据 Topic
     */
    public static final String TOPIC_MEDIA_STATS = "media-stats";

    /**
     * 房间消息Topic
     */
    public static final String ROOM_MESSAGE_TOPIC = "room-message";
    /**
     * 房间录制Topic
     */
    public static final String ROOM_RECORD_TOPIC = "room-record";
    /**
     * 参与者事件主题
     */
    public static final String TOPIC_PARTICIPANT_EVENT = "participant-event";


    // ==================== Tag ====================
    /**
     * 房间消息Tag
     */
    public static final String ROOM_MESSAGE_TAG = "send";

    // ==================== 录制相关 ====================
    public static final String TAG_RECORDING_STARTED = "started";

    public static final String TAG_RECORDING_COMPLETED = "completed";

    public static final String TAG_RECORDING_FAILED = "failed";

    /**
     * 参与者加入事件 Tag
     */
    public static final String TAG_PARTICIPANT_JOINED = "joined";

    /**
     * 参与者离开事件 Tag
     */
    public static final String TAG_PARTICIPANT_LEFT = "left";

    /**
     * 媒体统计上报 Tag
     */
    public static final String TAG_STATS_REPORT = "stats-report";

    /**
     * 房间创建事件 Tag
     */
    public static final String TAG_ROOM_CREATED = "room-created";

    /**
     * 房间关闭事件 Tag
     */
    public static final String TAG_ROOM_CLOSED = "room-closed";

    /**
     * 房间分配 SFU 节点事件 Tag
     */
    public static final String TAG_ROOM_SFU_ASSIGNED = "sfu-assigned";


}
