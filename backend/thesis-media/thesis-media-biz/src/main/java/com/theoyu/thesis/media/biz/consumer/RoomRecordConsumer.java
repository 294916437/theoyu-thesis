package com.theoyu.thesis.media.biz.consumer;

import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import com.theoyu.thesis.media.biz.model.dto.RecordingCompletedEventDTO;
import com.theoyu.thesis.media.biz.model.dto.RecordingStartedEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.ROOM_RECORD_TOPIC,
        consumerGroup = "thesis-media-recording-group"
)
public class RoomRecordConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt message) {
        String tags = message.getTags();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("[RocketMQ] 收到录制消息 | Tag: {} | MsgId: {}", tags, message.getMsgId());

        try {
            switch (tags) {
                case MQConstants.TAG_RECORDING_STARTED:
                    handleRecordingStarted(body);
                    break;
                case MQConstants.TAG_RECORDING_COMPLETED:
                    handleRecordingCompleted(body);
                    break;
                case MQConstants.TAG_RECORDING_FAILED:
                    handleRecordingFailed(body);
                    break;
                default:
                    log.warn("[RocketMQ] 未知的录制标签: {}", tags);
            }
        } catch (Exception e) {
            log.error("[RocketMQ] 处理录制消息异常", e);
            // 这里可以根据业务决定是否 throw e 让 MQ 重试
        }
    }

    /**
     * 处理录制开始事件
     * 业务逻辑：例如通知推送、更新房间扩展信息状态等
     */
    private void handleRecordingStarted(String jsonBody) {
        RecordingStartedEventDTO event = JsonUtils.parseObject(jsonBody, RecordingStartedEventDTO.class);
        if (event == null) {
            log.error("[RecordingConsumer] 解析开始事件失败");
            return;
        }

        log.info(">>> 录制已开始: roomId={}, hostId={}, format={}, time={}",
                event.getRoomId(), event.getHostId(), event.getFormat(), event.getStartTime());

        // TODO: 可以在此处扩展业务，例如发送 WebSocket 通知房间全员 "录制已开始"
    }

    /**
     * 处理录制完成事件
     * 业务逻辑：生成回放记录、触发云端转码、计算账单
     */
    private void handleRecordingCompleted(String jsonBody) {
        RecordingCompletedEventDTO event = JsonUtils.parseObject(jsonBody, RecordingCompletedEventDTO.class);
        if (event == null) {
            log.error("[RecordingConsumer] 解析完成事件失败");
            return;
        }

        log.info(">>> 录制已完成: roomId={}, fileUrl={}, duration={}s, size={} bytes",
                event.getRoomId(), event.getFileUrl(), event.getDuration(), event.getFileSize());

        // 使用 JsonUtils 打印完整详情用于审计
        if (log.isDebugEnabled()) {
            log.debug("录制元数据: {}", JsonUtils.toJsonString(event));
        }

        // TODO: 可在此处调用点播服务(VOD)进行文件处理或归档
    }

    /**
     * 处理录制失败事件
     * 业务逻辑：告警通知、补偿重试
     */
    private void handleRecordingFailed(String jsonBody) {
        // 简单使用 Map 接收未知结构的错误信息，利用 MapUtils/JsonUtils 能力
        try {
            // 假设失败消息体可能是一个简单的 Map 或者特定的 ErrorDTO，此处以 Map 通用处理
            Object errorInfo = JsonUtils.parseObject(jsonBody, Object.class);
            log.error(">>> 录制异常告警: {}", JsonUtils.toJsonString(errorInfo));

            // TODO: 集成钉钉/飞书告警
        } catch (Exception e) {
            log.error("解析失败消息体出错", e);
        }
    }

}
