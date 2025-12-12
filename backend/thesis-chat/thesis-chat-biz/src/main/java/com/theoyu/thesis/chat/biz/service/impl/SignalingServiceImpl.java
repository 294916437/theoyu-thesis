package com.theoyu.thesis.chat.biz.service.impl;

import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.chat.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.chat.biz.model.entity.SignalMessage;
import com.theoyu.thesis.chat.biz.service.SignalingService;
import com.theoyu.thesis.chat.biz.service.UserOnlineService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 信令服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalingServiceImpl implements SignalingService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserOnlineService userOnlineService;

    @Override
    public void handleCallOffer(SignalMessage.CallOffer offer) {
        try {
            Long toUserId = offer.getToUserId();

            // 使用独立的在线状态服务检查
            if (!userOnlineService.isUserOnline(toUserId)) {
                log.warn("目标用户不在线 - toUserId: {}, callId: {}", toUserId, offer.getCallId());
                sendErrorToUser(offer.getFromUserId(), "对方不在线，无法发起通话");
                return;
            }

            offer.setTimestamp(System.currentTimeMillis());
            saveCallStatus(offer);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(toUserId),
                    "/queue/call-offer",
                    offer
            );

            log.info("转发通话邀请 - from: {}, to: {}, callId: {}",
                    offer.getFromUserId(), toUserId, offer.getCallId());

        } catch (Exception e) {
            log.error("处理通话邀请失败 - callId: {}", offer.getCallId(), e);
            sendErrorToUser(offer.getFromUserId(), "服务器错误，请稍后重试");
        }
    }

    @Override
    public void handleCallAnswer(SignalMessage.CallAnswer answer) {
        try {
            Long toUserId = answer.getToUserId();
            answer.setTimestamp(System.currentTimeMillis());
            updateCallStatus(answer);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(toUserId),
                    "/queue/call-answer",
                    answer
            );

            log.info("转发通话应答 - from: {}, to: {}, callId: {}",
                    answer.getFromUserId(), toUserId, answer.getCallId());

        } catch (Exception e) {
            log.error("处理通话应答失败 - callId: {}", answer.getCallId(), e);
        }
    }

    @Override
    public void handleIceCandidate(SignalMessage.IceCandidate candidate) {
        try {
            Long toUserId = candidate.getToUserId();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(toUserId),
                    "/queue/ice-candidate",
                    candidate
            );

            log.debug("转发 ICE 候选 - from: {}, to: {}, callId: {}",
                    candidate.getFromUserId(), toUserId, candidate.getCallId());

        } catch (Exception e) {
            log.error("处理 ICE 候选失败 - callId: {}", candidate.getCallId(), e);
        }
    }

    @Override
    public void handleCallEnd(SignalMessage.CallEnd callEnd) {
        try {
            Long toUserId = callEnd.getToUserId();
            callEnd.setTimestamp(System.currentTimeMillis());
            deleteCallStatus(callEnd.getCallId());

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(toUserId),
                    "/queue/call-end",
                    callEnd
            );

            log.info("通话结束 - from: {}, to: {}, callId: {}, reason: {}",
                    callEnd.getFromUserId(), toUserId, callEnd.getCallId(), callEnd.getReason());

        } catch (Exception e) {
            log.error("处理通话结束失败 - callId: {}", callEnd.getCallId(), e);
        }
    }

    private void saveCallStatus(SignalMessage.CallOffer offer) {
        String callKey = RedisKeyConstants.buildCallStatusKey(offer.getCallId());
        String callData = JSON.toJSONString(offer);
        redisTemplate.opsForValue().set(callKey, callData, 1, TimeUnit.HOURS);
    }

    private void updateCallStatus(SignalMessage.CallAnswer answer) {
        String callKey = RedisKeyConstants.buildCallStatusKey(answer.getCallId());
        String callData = JSON.toJSONString(answer);
        redisTemplate.opsForValue().set(callKey, callData, 1, TimeUnit.HOURS);
    }

    private void deleteCallStatus(String callId) {
        String callKey = RedisKeyConstants.buildCallStatusKey(callId);
        redisTemplate.delete(callKey);
    }

    private void sendErrorToUser(Long userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/error",
                errorMessage
        );
    }
}