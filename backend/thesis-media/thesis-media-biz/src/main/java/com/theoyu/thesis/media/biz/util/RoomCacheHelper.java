package com.theoyu.thesis.media.biz.util;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.theoyu.thesis.media.biz.model.entity.RoomPO;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 房间缓存工具类 - 使用 Hash 结构优化存储
 *
 * @author theoyu
 */
public class RoomCacheHelper {

    /**
     * 将 RoomPO 转换为 Hash Map
     */
    public static Map<String, String> roomToHashMap(RoomPO room) {
        Map<String, String> hashMap = new HashMap<>();

        hashMap.put("id", String.valueOf(room.getId()));
        hashMap.put("roomNo", room.getRoomNo());
        hashMap.put("hostId", String.valueOf(room.getHostId()));

        if (room.getSfuNodeId() != null) {
            hashMap.put("sfuNodeId", String.valueOf(room.getSfuNodeId()));
        }

        hashMap.put("title", room.getTitle());
        hashMap.put("type", String.valueOf(room.getType()));
        hashMap.put("maxParticipants", String.valueOf(room.getMaxParticipants()));
        hashMap.put("status", String.valueOf(room.getStatus()));

        if (room.getStartTime() != null) {
            hashMap.put("startTime", String.valueOf(localDateTimeToTimestamp(room.getStartTime())));
        }

        if (room.getEndTime() != null) {
            hashMap.put("endTime", String.valueOf(localDateTimeToTimestamp(room.getEndTime())));
        }

        if (room.getCreatedTime() != null) {
            hashMap.put("createdTime", String.valueOf(localDateTimeToTimestamp(room.getCreatedTime())));
        }

        if (room.getUpdatedTime() != null) {
            hashMap.put("updatedTime", String.valueOf(localDateTimeToTimestamp(room.getUpdatedTime())));
        }

        if (room.getSettings() != null) {
            hashMap.put("settings", room.getSettings());
        }

        return hashMap;
    }

    /**
     * 从 Hash Map 转换为 RoomPO
     */
    public static RoomPO hashMapToRoom(Map<Object, Object> hashMap) {
        if (hashMap == null || hashMap.isEmpty()) {
            return null;
        }

        RoomPO room = new RoomPO();

        room.setId(Long.valueOf(hashMap.get("id").toString()));
        room.setRoomNo(hashMap.get("roomNo").toString());
        room.setHostId(Long.valueOf(hashMap.get("hostId").toString()));

        if (hashMap.containsKey("sfuNodeId")) {
            room.setSfuNodeId(Long.valueOf(hashMap.get("sfuNodeId").toString()));
        }

        room.setTitle(hashMap.get("title").toString());
        room.setType(Integer.valueOf(hashMap.get("type").toString()));
        room.setMaxParticipants(Integer.valueOf(hashMap.get("maxParticipants").toString()));
        room.setStatus(Integer.valueOf(hashMap.get("status").toString()));

        if (hashMap.containsKey("startTime")) {
            room.setStartTime(timestampToLocalDateTime(Long.valueOf(hashMap.get("startTime").toString())));
        }

        if (hashMap.containsKey("endTime")) {
            room.setEndTime(timestampToLocalDateTime(Long.valueOf(hashMap.get("endTime").toString())));
        }

        if (hashMap.containsKey("createdTime")) {
            room.setCreatedTime(timestampToLocalDateTime(Long.valueOf(hashMap.get("createdTime").toString())));
        }

        if (hashMap.containsKey("updatedTime")) {
            room.setUpdatedTime(timestampToLocalDateTime(Long.valueOf(hashMap.get("updatedTime").toString())));
        }

        if (hashMap.containsKey("settings")) {
            room.setSettings(hashMap.get("settings").toString());
        }

        return room;
    }

    /**
     * LocalDateTime 转时间戳（毫秒）
     */
    private static long localDateTimeToTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 时间戳（毫秒）转 LocalDateTime
     */
    private static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }
}