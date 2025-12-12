package com.theoyu.thesis.chat.biz.service;

import com.theoyu.thesis.chat.biz.model.entity.SignalMessage;

/**
 * 信令服务接口（简化版，移除在线状态管理）
 */
public interface SignalingService {

    /**
     * 处理通话邀请
     */
    void handleCallOffer(SignalMessage.CallOffer offer);

    /**
     * 处理通话应答
     */
    void handleCallAnswer(SignalMessage.CallAnswer answer);

    /**
     * 处理 ICE 候选
     */
    void handleIceCandidate(SignalMessage.IceCandidate candidate);

    /**
     * 处理通话结束
     */
    void handleCallEnd(SignalMessage.CallEnd callEnd);
}