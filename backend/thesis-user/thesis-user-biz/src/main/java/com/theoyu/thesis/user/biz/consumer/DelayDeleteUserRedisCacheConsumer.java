package com.theoyu.thesis.user.biz.consumer;

import com.theoyu.thesis.user.biz.constants.MQConstants;
import com.theoyu.thesis.user.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.user.biz.service.impl.UserServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "user-cache-group", // Group
        topic = MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE, // 消费的主题 Topic
        messageModel = MessageModel.BROADCASTING
)
public class DelayDeleteUserRedisCacheConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String body) {
        Long userId = Long.valueOf(body);
        log.info("## 延迟消息消费成功, userId: {}", userId);

        // 删除 Redis 用户缓存
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
        String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);
        // 批量删除
        redisTemplate.delete(Arrays.asList(userInfoRedisKey, userProfileRedisKey));
        UserServiceImpl.invalidateLocalCache(userId);
    }
}
