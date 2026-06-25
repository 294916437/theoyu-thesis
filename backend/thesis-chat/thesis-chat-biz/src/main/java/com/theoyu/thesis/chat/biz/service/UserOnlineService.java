package com.theoyu.thesis.chat.biz.service;

import com.theoyu.thesis.chat.biz.constants.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态管理服务
 */
@Slf4j
@Service
public class UserOnlineService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final long ONLINE_TIMEOUT = 5;
    private static final String USER_ONLINE_SESSIONS_PREFIX = "user:online:sessions:";

    /**
     * 用户上线
     */
    public void setUserOnline(Long userId) {
        setUserOnline(userId, "default");
    }

    /**
     * 用户上线
     */
    public void setUserOnline(Long userId, String sessionId) {
        try {
            String onlineKey = RedisKeyConstants.buildUserOnlineKey(userId);
            String sessionsKey = buildUserOnlineSessionsKey(userId);
            long now = System.currentTimeMillis();
            redisTemplate.opsForHash().put(sessionsKey, sessionId, String.valueOf(now));
            redisTemplate.expire(sessionsKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(
                    onlineKey,
                    String.valueOf(now),
                    ONLINE_TIMEOUT,
                    TimeUnit.MINUTES
            );

        } catch (Exception e) {
            log.error("设置用户在线状态失败", e);
        }
    }

    /**
     * 用户下线
     */
    public void setUserOffline(Long userId) {
        setUserOffline(userId, "default");
    }

    /**
     * 用户下线
     */
    public void setUserOffline(Long userId, String sessionId) {
        try {
            String onlineKey = RedisKeyConstants.buildUserOnlineKey(userId);
            String sessionsKey = buildUserOnlineSessionsKey(userId);
            redisTemplate.opsForHash().delete(sessionsKey, sessionId);
            Long activeSessions = redisTemplate.opsForHash().size(sessionsKey);
            if (activeSessions == null || activeSessions == 0) {
                redisTemplate.delete(onlineKey);
                redisTemplate.delete(sessionsKey);
            } else {
                redisTemplate.expire(sessionsKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
                redisTemplate.expire(onlineKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
            }

            log.info("用户下线 - userId: {}, sessionId: {}, activeSessions: {}", userId, sessionId, activeSessions);
        } catch (Exception e) {
            log.error(" 清除用户在线状态失败", e);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        String onlineKey = RedisKeyConstants.buildUserOnlineKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey));
    }

    private String buildUserOnlineSessionsKey(Long userId) {
        return USER_ONLINE_SESSIONS_PREFIX + userId;
    }
}
