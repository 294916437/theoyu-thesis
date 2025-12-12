package com.theoyu.thesis.user.biz.service;

import com.theoyu.thesis.user.biz.constants.RedisKeyConstants;
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

    /**
     * 用户上线
     */
    public void setUserOnline(Long userId) {
        try {

            String onlineKey = RedisKeyConstants.buildUserOnlineKey(userId);
            redisTemplate.opsForValue().set(
                    onlineKey,
                    String.valueOf(System.currentTimeMillis()),
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
        try {
            String onlineKey = RedisKeyConstants.buildUserOnlineKey(userId);
            redisTemplate.delete(onlineKey);

            log.info("用户下线 - userId: {}", userId);
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
}