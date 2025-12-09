package com.theoyu.thesis.media.biz.service.impl;

import com.theoyu.thesis.media.biz.rpc.OssRpcService;
import com.theoyu.thesis.media.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.media.biz.service.MediaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MediaServiceImpl implements MediaService {
    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

}
