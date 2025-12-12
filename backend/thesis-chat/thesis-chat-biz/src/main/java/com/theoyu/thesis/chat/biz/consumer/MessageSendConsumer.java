package com.theoyu.thesis.chat.biz.consumer;

import com.theoyu.thesis.chat.biz.constants.MQConstants;
import com.theoyu.thesis.chat.biz.model.dto.MessageSendDTO;
import com.theoyu.thesis.chat.biz.service.MessagePushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = MQConstants.CONSUMER_GROUP_MESSAGE_SEND,
    topic = MQConstants.TOPIC_MESSAGE_SEND,
    selectorExpression = MQConstants.TAG_MESSAGE_SEND
)
public class MessageSendConsumer implements RocketMQListener<MessageSendDTO> {
    
    @Resource
    private MessagePushService messagePushService;
    
    @Override
    public void onMessage(MessageSendDTO message) {
        log.info("==> RocketMQ【消费对话消息】, messageId: {}, conversationId: {}",
            message.getMessageId(), message.getConversationId());
        
        try {
            // 通过WebSocket推送给在线用户
            messagePushService.pushMessageToUsers(message);
        } catch (Exception e) {
            log.error("==> 消费消息发送MQ失败, messageId: {}", message.getMessageId(), e);
            // 抛出异常触发重试
            throw e;
        }
    }
}