package com.theoyu.thesis.chat.biz.service.impl;

import com.theoyu.thesis.chat.biz.model.dto.MessageSendDTO;
import com.theoyu.thesis.chat.biz.model.vo.MessageVO;
import com.theoyu.thesis.chat.biz.rpc.UserRpcService;
import com.theoyu.thesis.chat.biz.service.MessagePushService;
import com.theoyu.thesis.chat.biz.service.UserOnlineService;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class MessagePushServiceImpl implements MessagePushService {

    @Resource
    private SimpMessagingTemplate messagingTemplate;
    @Resource
    private UserOnlineService userOnlineService;
    @Resource
    private UserRpcService userRpcService;


    @Override
    public void pushMessageToUsers(MessageSendDTO message) {
        try {
            // 先检查在线状态，避免无效推送
            List<Long> onlineReceivers = message.getReceiverIds().stream()
                    .filter(userId -> userOnlineService.isUserOnline(userId))
                    .toList();

            if (onlineReceivers.isEmpty()) {
                log.debug("所有接收者离线, messageId: {}", message.getMessageId());
                return;
            }

            // 查询发送者信息（只查一次）
            Map<Long, FindUserByIdRspDTO> userInfoMap =
                    userRpcService.findByIds2(Set.of(message.getSenderId()));

            FindUserByIdRspDTO senderInfo = userInfoMap.get(message.getSenderId());

            // 构建推送消息
            MessageVO pushMessage = MessageVO.builder()
                    .id(message.getMessageId())
                    .conversationId(message.getConversationId())
                    .senderId(message.getSenderId())
                    .senderNickname(senderInfo != null ? senderInfo.getNickName() : "")
                    .senderAvatar(senderInfo != null ? senderInfo.getAvatar() : "")
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .imgUris(message.getImgUris())
                    .videoUri(message.getVideoUri())
                    .createdTime(message.getSendTime())
                    .isSelf(false)
                    .build();

            // 批量推送
            for (Long receiverId : onlineReceivers) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(receiverId),
                            "/queue/message-receive",
                            pushMessage
                    );
                } catch (Exception e) {
                    log.warn("推送失败, receiverId: {}, messageId: {}",
                            receiverId, message.getMessageId(), e);
                }
            }

        } catch (Exception e) {
            log.error("WebSocket推送失败, messageId: {}", message.getMessageId(), e);
        }
    }
}