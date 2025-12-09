package com.theoyu.thesis.media.biz.consumer;

import com.alibaba.fastjson.JSON;
import com.theoyu.thesis.media.biz.constants.MQConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 媒体统计数据消费者
 *
 * 业务职责:
 * 1. 分析媒体质量指标（丢包率、延迟、码率等）
 * 2. 检测异常情况并告警
 * 3. 聚合统计数据用于监控
 * 4. 存储到时序数据库（可选）
 * 5. 触发自适应码率调整建议（可选）
 *
 * @author theoyu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_MEDIA_STATS,
        selectorExpression = MQConstants.TAG_STATS_REPORT,
        consumerGroup = "media-stats-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 2 // 统计数据允许丢失，重试次数少一些
)
public class MediaStatsConsumer extends BaseRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 质量指标阈值
     */
    private static final double PACKET_LOSS_THRESHOLD = 5.0;      // 丢包率阈值 5%
    private static final long RTT_THRESHOLD = 300;                 // RTT阈值 300ms
    private static final long JITTER_THRESHOLD = 50;               // 抖动阈值 50ms
    private static final long BITRATE_MIN_THRESHOLD = 100 * 1024;  // 最低码率 100kbps

    @Override
    public void onMessage(String message) {
        log.debug("[MediaStatsConsumer] Received message");

        try {
            // 1. 解析消息
            Map<String, Object> msgMap = JSON.parseObject(message, Map.class);
            String roomId = msgMap.get("roomId").toString();
            String peerId = msgMap.get("peerId").toString();
            Map<String, Object> stats = (Map<String, Object>) msgMap.get("stats");
            String timestamp = msgMap.get("timestamp").toString();

            // 2. 幂等性检查（基于时间戳，允许同一时刻多个统计数据）
            String idempotentKey = generateIdempotentKey(
                    MQConstants.TOPIC_MEDIA_STATS,
                    MQConstants.TAG_STATS_REPORT,
                    roomId + ":" + peerId + ":" + timestamp
            );

            Boolean isFirstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 1, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                log.debug("[MediaStatsConsumer] Duplicate message - roomId: {}, peerId: {}",
                        roomId, peerId);
                return;
            }

            // 3. 执行核心业务逻辑
            processMediaStats(roomId, peerId, stats, timestamp);

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Process failed", e);
            // 统计数据处理失败不抛异常，避免阻塞队列
            // throw new RuntimeException("Media stats consume failed", e);
        }
    }

    /**
     * 处理媒体统计数据核心逻辑
     */
    private void processMediaStats(String roomId, String peerId,
                                   Map<String, Object> stats, String timestamp) {
        // 1. 分析质量指标
        QualityMetrics metrics = analyzeQualityMetrics(stats);

        // 2. 检测异常情况
        if (detectAnomalies(roomId, peerId, metrics)) {
            // 异步发送告警
            taskExecutor.execute(() -> sendQualityAlert(roomId, peerId, metrics));
        }

        // 3. 聚合房间级别统计数据
        taskExecutor.execute(() -> aggregateRoomStats(roomId, metrics, timestamp));

        // 4. 存储peer级别统计数据（用于历史查询）
        taskExecutor.execute(() -> storePeerStats(roomId, peerId, stats, timestamp));

        // 5. 更新实时质量评分
        taskExecutor.execute(() -> updateQualityScore(roomId, peerId, metrics));

        log.debug("[MediaStatsConsumer] Stats processed - roomId: {}, peerId: {}", roomId, peerId);
    }

    /**
     * 分析质量指标
     */
    private QualityMetrics analyzeQualityMetrics(Map<String, Object> stats) {
        QualityMetrics metrics = new QualityMetrics();

        try {
            // 解析各项指标（根据实际stats结构调整）
            if (stats.containsKey("packetLoss")) {
                metrics.packetLoss = Double.parseDouble(stats.get("packetLoss").toString());
            }
            if (stats.containsKey("rtt")) {
                metrics.rtt = Long.parseLong(stats.get("rtt").toString());
            }
            if (stats.containsKey("jitter")) {
                metrics.jitter = Long.parseLong(stats.get("jitter").toString());
            }
            if (stats.containsKey("bitrate")) {
                metrics.bitrate = Long.parseLong(stats.get("bitrate").toString());
            }
            if (stats.containsKey("frameRate")) {
                metrics.frameRate = Integer.parseInt(stats.get("frameRate").toString());
            }
            if (stats.containsKey("resolution")) {
                metrics.resolution = stats.get("resolution").toString();
            }

            // 计算综合质量评分 (0-100)
            metrics.qualityScore = calculateQualityScore(metrics);

        } catch (Exception e) {
            log.warn("[MediaStatsConsumer] Failed to parse quality metrics", e);
        }

        return metrics;
    }

    /**
     * 计算质量评分
     */
    private int calculateQualityScore(QualityMetrics metrics) {
        int score = 100;

        // 丢包率扣分
        if (metrics.packetLoss > 0) {
            score -= (int) (metrics.packetLoss * 5); // 每1%丢包扣5分
        }

        // RTT扣分
        if (metrics.rtt > 100) {
            score -= (int) ((metrics.rtt - 100) / 50); // 每增加50ms扣1分
        }

        // 抖动扣分
        if (metrics.jitter > 30) {
            score -= (int) ((metrics.jitter - 30) / 10); // 每增加10ms扣1分
        }

        // 码率扣分
        if (metrics.bitrate < 500 * 1024) {
            score -= 10; // 低码率扣10分
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 检测异常情况
     */
    private boolean detectAnomalies(String roomId, String peerId, QualityMetrics metrics) {
        boolean hasAnomaly = false;

        // 1. 丢包率过高
        if (metrics.packetLoss > PACKET_LOSS_THRESHOLD) {
            log.warn("[MediaStatsConsumer] High packet loss detected - roomId: {}, peerId: {}, " +
                    "loss: {}%", roomId, peerId, metrics.packetLoss);
            hasAnomaly = true;
        }

        // 2. RTT过高
        if (metrics.rtt > RTT_THRESHOLD) {
            log.warn("[MediaStatsConsumer] High RTT detected - roomId: {}, peerId: {}, " +
                    "rtt: {}ms", roomId, peerId, metrics.rtt);
            hasAnomaly = true;
        }

        // 3. 抖动过大
        if (metrics.jitter > JITTER_THRESHOLD) {
            log.warn("[MediaStatsConsumer] High jitter detected - roomId: {}, peerId: {}, " +
                    "jitter: {}ms", roomId, peerId, metrics.jitter);
            hasAnomaly = true;
        }

        // 4. 码率过低
        if (metrics.bitrate < BITRATE_MIN_THRESHOLD) {
            log.warn("[MediaStatsConsumer] Low bitrate detected - roomId: {}, peerId: {}, " +
                    "bitrate: {} kbps", roomId, peerId, metrics.bitrate / 1024);
            hasAnomaly = true;
        }

        // 5. 质量评分过低
        if (metrics.qualityScore < 50) {
            log.warn("[MediaStatsConsumer] Low quality score - roomId: {}, peerId: {}, " +
                    "score: {}", roomId, peerId, metrics.qualityScore);
            hasAnomaly = true;
        }

        return hasAnomaly;
    }

    /**
     * 发送质量告警
     */
    private void sendQualityAlert(String roomId, String peerId, QualityMetrics metrics) {
        try {
            // 检查是否在告警抑制期（避免频繁告警）
            String alertKey = String.format("media:alert:quality:%s:%s", roomId, peerId);
            Boolean alreadyAlerted = redisTemplate.hasKey(alertKey);

            if (Boolean.TRUE.equals(alreadyAlerted)) {
                log.debug("[MediaStatsConsumer] Alert suppressed - roomId: {}, peerId: {}",
                        roomId, peerId);
                return;
            }

            // 记录告警信息
            Map<String, Object> alert = new HashMap<>();
            alert.put("roomId", roomId);
            alert.put("peerId", peerId);
            alert.put("timestamp", System.currentTimeMillis());
            alert.put("metrics", metrics);
            alert.put("message", "媒体质量异常");

            // 存储告警记录
            String alertRecordKey = String.format("media:alerts:%s", roomId);
            redisTemplate.opsForList().leftPush(alertRecordKey, JSON.toJSONString(alert));
            redisTemplate.expire(alertRecordKey, 7, TimeUnit.DAYS);

            // 设置告警抑制（5分钟内不重复告警）
            redisTemplate.opsForValue().set(alertKey, "1", 5, TimeUnit.MINUTES);

            log.info("[MediaStatsConsumer] Quality alert sent - roomId: {}, peerId: {}, score: {}",
                    roomId, peerId, metrics.qualityScore);

            // TODO: 集成实际的告警系统（邮件、短信、钉钉、Prometheus等）
            // alertService.sendAlert(alert);

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Failed to send quality alert - roomId: {}, peerId: {}",
                    roomId, peerId, e);
        }
    }

    /**
     * 聚合房间统计数据
     */
    private void aggregateRoomStats(String roomId, QualityMetrics metrics, String timestamp) {
        try {
            String aggKey = String.format("media:room:stats:agg:%s", roomId);

            // 使用Hash存储聚合数据
            redisTemplate.opsForHash().increment(aggKey, "totalReports", 1);
            redisTemplate.opsForHash().increment(aggKey, "totalPacketLoss",
                    (long) (metrics.packetLoss * 100)); // 保留2位小数
            redisTemplate.opsForHash().increment(aggKey, "totalRtt", metrics.rtt);
            redisTemplate.opsForHash().increment(aggKey, "totalJitter", metrics.jitter);
            redisTemplate.opsForHash().increment(aggKey, "totalBitRate", metrics.bitrate);
            redisTemplate.opsForHash().increment(aggKey, "totalQualityScore", metrics.qualityScore);

            // 更新最后更新时间
            redisTemplate.opsForHash().put(aggKey, "lastUpdateTime", timestamp);

            // 设置过期时间
            redisTemplate.expire(aggKey, 7, TimeUnit.DAYS);

            log.debug("[MediaStatsConsumer] Room stats aggregated - roomId: {}", roomId);

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Failed to aggregate room stats - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 存储peer级别统计数据
     */
    private void storePeerStats(String roomId, String peerId,
                                Map<String, Object> stats, String timestamp) {
        try {
            // 使用Sorted Set存储时序数据（按时间戳排序）
            String statsKey = String.format("media:peer:stats:timeline:%s:%s", roomId, peerId);

            redisTemplate.opsForZSet().add(statsKey,
                    JSON.toJSONString(stats),
                    Double.parseDouble(timestamp));

            // 只保留最近1小时的数据
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
            redisTemplate.opsForZSet().removeRangeByScore(statsKey, 0, oneHourAgo);

            // 设置过期时间
            redisTemplate.expire(statsKey, 2, TimeUnit.HOURS);

            log.debug("[MediaStatsConsumer] Peer stats stored - roomId: {}, peerId: {}",
                    roomId, peerId);

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Failed to store peer stats - roomId: {}, peerId: {}",
                    roomId, peerId, e);
        }
    }

    /**
     * 更新实时质量评分
     */
    private void updateQualityScore(String roomId, String peerId, QualityMetrics metrics) {
        try {
            // 存储最新的质量评分（用于实时展示）
            String scoreKey = String.format("media:peer:quality:%s:%s", roomId, peerId);

            Map<String, Object> qualityInfo = new HashMap<>();
            qualityInfo.put("score", metrics.qualityScore);
            qualityInfo.put("packetLoss", metrics.packetLoss);
            qualityInfo.put("rtt", metrics.rtt);
            qualityInfo.put("jitter", metrics.jitter);
            qualityInfo.put("bitrate", metrics.bitrate);
            qualityInfo.put("updateTime", System.currentTimeMillis());

            redisTemplate.opsForValue().set(scoreKey, JSON.toJSONString(qualityInfo),
                    10, TimeUnit.MINUTES);

            // 更新房间平均质量评分
            updateRoomAverageQuality(roomId);

            log.debug("[MediaStatsConsumer] Quality score updated - roomId: {}, peerId: {}, score: {}",
                    roomId, peerId, metrics.qualityScore);

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Failed to update quality score - roomId: {}, peerId: {}",
                    roomId, peerId, e);
        }
    }

    /**
     * 更新房间平均质量评分
     */
    private void updateRoomAverageQuality(String roomId) {
        try {
            // 获取房间所有peer的质量评分
            String pattern = String.format("media:peer:quality:%s:*", roomId);
            var keys = redisTemplate.keys(pattern);

            if (keys.isEmpty()) {
                return;
            }

            int totalScore = 0;
            int count = 0;

            for (String key : keys) {
                String qualityJson = (String) redisTemplate.opsForValue().get(key);
                if (qualityJson != null) {
                    Map<String, Object> quality = JSON.parseObject(qualityJson, Map.class);
                    totalScore += Integer.parseInt(quality.get("score").toString());
                    count++;
                }
            }

            if (count > 0) {
                int avgScore = totalScore / count;
                String avgKey = String.format("media:room:quality:avg:%s", roomId);
                redisTemplate.opsForValue().set(avgKey, avgScore, 10, TimeUnit.MINUTES);

                log.debug("[MediaStatsConsumer] Room average quality updated - roomId: {}, " +
                        "avgScore: {}", roomId, avgScore);
            }

        } catch (Exception e) {
            log.error("[MediaStatsConsumer] Failed to update room average quality - roomId: {}",
                    roomId, e);
        }
    }

    /**
     * 质量指标数据类
     */
    private static class QualityMetrics {
        double packetLoss = 0.0;    // 丢包率 (%)
        long rtt = 0;                // 往返时延 (ms)
        long jitter = 0;             // 抖动 (ms)
        long bitrate = 0;            // 码率 (bps)
        int frameRate = 0;           // 帧率 (fps)
        String resolution = "";      // 分辨率
        int qualityScore = 100;      // 质量评分 (0-100)
    }
}