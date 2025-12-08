package com.theoyu.thesis.media.biz.consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * 消费者基类 - 提供通用功能
 */
@Slf4j
public abstract class BaseRocketMQConsumer {

    /**
     * 生成消息幂等性Key
     */
    protected String generateIdempotentKey(String topic, String tag, String messageId) {
        return String.format("mq:idempotent:%s:%s:%s", topic, tag, messageId);
    }
}