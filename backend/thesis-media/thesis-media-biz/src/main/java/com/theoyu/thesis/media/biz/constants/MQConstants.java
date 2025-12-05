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

    // ==================== Tag ====================

    /**
     * 参与者加入事件 Tag
     */
    public static final String TAG_PARTICIPANT_JOINED = "participant-joined";

    /**
     * 参与者离开事件 Tag
     */
    public static final String TAG_PARTICIPANT_LEFT = "participant-left";

    /**
     * 媒体统计上报 Tag
     */
    public static final String TAG_STATS_REPORT = "stats-report";
}
