package com.theoyu.thesis.chat.biz.service;

import com.theoyu.thesis.chat.biz.model.dto.MessageSendDTO;

public interface MessagePushService {
    /**
     * 通过WebSocket推送消息给在线用户
     */
    void pushMessageToUsers(MessageSendDTO message);
}