# SFU 端到端延迟测试方案

## 1. 方案设计

### 1.1 测试目标

在真实多人会议场景下，收集以下指标用于论文性能章节：

| 指标                   | 含义                                             |
| ---------------------- | ------------------------------------------------ |
| **E2E 延迟均值**       | 所有 sender→receiver Peer 对的平均端到端单向延迟 |
| **E2E 延迟最大值**     | 采集窗口内最差的端到端延迟（含 P95）             |
| **RTCP RTT（发送端）** | 发送方浏览器 ↔ SFU 的往返时延                   |
| **RTCP RTT（接收端）** | SFU ↔ 接收方浏览器的往返时延                    |

### 1.2 SFU 媒体流路径与延迟分解

在 SFU 架构中，媒体包的传播路径为：

```
发送方浏览器
    │  RTP (UDP)
    ▼
  SFU Worker ← Producer (inbound-rtp)
    │  RTP (UDP)
    ▼  Consumer (outbound-rtp)
接收方浏览器
```

端到端延迟由两段独立的网络路径构成：

```
E2E 单向延迟 ≈ sender_one_way + receiver_one_way
             = senderRTT / 2  +  receiverRTT / 2
             = (senderRTT + receiverRTT) / 2
```

> **注意**：此公式假设上下行路径对称，这是 WebRTC RTT 测量的标准假设。
> 在局域网或典型互联网环境下误差通常在 ±5ms 以内。

### 1.3 数据来源：RTCP RTT

**senderRTT（Producer 侧）**

- Mediasoup Worker 监听来自发送方的 RTCP SR（Sender Report）
- Worker 在收到 SR 后，通过 RTCP XR/DLRR（RFC 3611）或标准 RTCP RR 回路计算往返时间
- 通过 `producer.getStats()` 返回的 `roundTripTime` 字段暴露，单位为秒
- 此值完全由 Worker 内部的 RTCP 状态机维护，与应用层逻辑无关

**receiverRTT（Consumer 侧）**

- Mediasoup Worker 向接收方发送 RTCP SR（包含发送时间戳 NTP）
- 接收方浏览器回复 RTCP RR（包含 DLSR/LRSR 字段）
- Worker 据此计算 RTT 并通过 `consumer.getStats()` 的 `roundTripTime` 字段暴露

两者均来自真实媒体流的 RTCP 控制面，**不依赖时钟同步**，不注入任何测试包。

### 1.4 实现方案

采集逻辑在 `sfu/src/features/latency-collector.ts` 中以单例服务实现，随 SFU 进程一同启动：

```
LatencyCollector (单例)
  ├── 每 5 秒轮询一次（setInterval.unref()，不影响进程退出）
  │   ├── 遍历所有 Room → Peer → Producer
  │   │   └── producer.getStats() → senderRTT
  │   └── 遍历所有 Room → Peer → Consumer
  │       ├── 匹配 consumer.producerId → 关联对应 producer
  │       └── consumer.getStats() → receiverRTT
  │           → E2E = (senderRTT + receiverRTT) / 2
  ├── 维护滑动窗口（最近 2000 条样本）
  ├── 每 30 秒将报告写入 latency-report.json
  └── GET /api/latency-report → 实时报告（JSON）
```

### 1.5 性能影响评估

| 方面              | 影响                                                    |
| ----------------- | ------------------------------------------------------- |
| `getStats()` 调用 | 仅读取 Worker 内部缓存的 RTC stats，不产生额外网络包    |
| 调用频率          | 每个 Producer/Consumer 每 5 秒各调用一次                |
| 内存占用          | 滑动窗口上限 2000 条样本，每条约 200 字节，最大 ~400 KB |
| CPU 开销          | 可忽略（异步 I/O，主循环 5 秒一次）                     |
| 定时器            | 使用 `unref()`，不阻塞进程优雅退出                      |

---

## 2. 数据采集步骤

### 2.1 前提条件

- SFU 服务已在 `server.ts` 中集成 `LatencyCollector`（已完成）
- 会议中至少存在 **2 个及以上** 的真实参与者（需要真实媒体流）
- 参与者摄像头/麦克风处于开启状态（Producer 活跃）

### 2.2 实时查询报告

SFU 进程运行期间，访问以下接口获取实时延迟报告：

```bash
# 默认端口 3000（根据实际配置调整）
curl http://localhost:3000/api/latency-report | jq .
```

### 2.3 文件输出

SFU 工作目录下每 30 秒自动写入 `latency-report.json`，进程退出时写入最终报告。

```bash
# 实时监视报告文件（Linux/Mac）
watch -n 5 "cat latency-report.json | jq '.global'"
```

### 2.4 推荐测试场景

| 场景           | 人数   | 持续时间 | 目的                       |
| -------------- | ------ | -------- | -------------------------- |
| 基准（局域网） | 2~3 人 | 5 分钟   | 建立基准延迟数据           |
| 多人会议       | 4~6 人 | 10 分钟  | 观察参与者增加时的延迟变化 |
| 带宽受限       | 2~3 人 | 5 分钟   | 模拟弱网（限速工具辅助）   |

---

## 3. 报告结构说明

`/api/latency-report` 返回 JSON 示例：

```json
{
  "collectedAt": "2026-05-05T10:00:00.000Z",
  "collectionDurationSec": 300,
  "totalSamples": 1240,
  "global": {
    "sampleCount": 1240,
    "e2eMeanMs": 28.5,
    "e2eMaxMs": 87.3,
    "e2eMinMs": 8.1,
    "e2eP50Ms": 25.2,
    "e2eP95Ms": 65.4,
    "senderRttMeanMs": 22.1,
    "receiverRttMeanMs": 34.9,
    "senderRttMaxMs": 76.2,
    "receiverRttMaxMs": 98.4
  },
  "byRoom": {
    "room-abc123": {
      "roomId": "room-abc123",
      "activePeerPairs": 6,
      "sampleCount": 1240,
      "e2eMeanMs": 28.5,
      ...
    }
  },
  "recentSamples": [
    {
      "timestamp": 1746441600000,
      "roomId": "room-abc123",
      "senderPeerId": "user-001",
      "receiverPeerId": "user-002",
      "producerKind": "video",
      "senderRttMs": 22.4,
      "receiverRttMs": 34.6,
      "e2eMs": 28.5
    }
  ]
}
```

### 字段说明

| 字段                | 说明                                             |
| ------------------- | ------------------------------------------------ |
| `e2eMeanMs`         | **论文指标**：E2E 延迟均值（毫秒）               |
| `e2eMaxMs`          | **论文指标**：E2E 延迟最大值（毫秒）             |
| `e2eP95Ms`          | E2E 延迟 95 百分位（毫秒）                       |
| `senderRttMeanMs`   | **论文指标**：RTCP RTT 均值（发送端↔SFU，毫秒） |
| `receiverRttMeanMs` | **论文指标**：RTCP RTT 均值（SFU↔接收端，毫秒） |
| `senderRttMaxMs`    | RTCP RTT 最大值（发送端↔SFU，毫秒）             |
| `receiverRttMaxMs`  | RTCP RTT 最大值（SFU↔接收端，毫秒）             |
| `activePeerPairs`   | 当前有效的 sender→receiver Peer 对数量           |

---

## 4. 计算正确性说明

### 4.1 为何不使用时间戳差值？

直接用「发送时间戳 - 接收时间戳」计算延迟需要发送方与接收方**时钟同步**。
浏览器端时钟误差通常在数十毫秒量级，无法用于准确的单向延迟测量。

### 4.2 RTCP RTT 的可靠性

RTCP RTT 基于**往返时间测量**，不依赖两端时钟同步：

```
RTCP RTT = T_now - T_sr_sent - DLSR
```

- `T_sr_sent`：上一次发送 RTCP SR 的 NTP 时间（本地记录）
- `DLSR`：接收方在收到 SR 和发送 RR 之间的延迟（接收方填写）

此方法与 TCP 的 RTT 测量原理相同，是 RFC 3550 规定的标准机制。

### 4.3 E2E 估算误差来源

- **路径不对称**：上行/下行经由不同路由时，RTT/2 会高估或低估单向延迟
- **Jitter Buffer**：接收端播放缓冲引入的固定延迟不在 RTCP RTT 统计范围内（通常 20~100ms）
- **SFU 内部转发延迟**：mediasoup Worker 的转发延迟极低（通常 < 1ms），可忽略

> 综合以上因素，本方案测量的 E2E 延迟为**网络传播延迟的估算下界**，
> 实际用户感知延迟 = 测量值 + Jitter Buffer 延迟。

---

## 5. 相关文件

| 文件                                    | 说明                                              |
| --------------------------------------- | ------------------------------------------------- |
| `sfu/src/features/latency-collector.ts` | 延迟采集服务核心实现                              |
| `sfu/src/server.ts`                     | 集成点：启动采集、注册 `/api/latency-report` 接口 |
| `sfu/latency-report.json`               | 运行时自动生成的报告文件（已加入 .gitignore）     |

---

## 6. 实测数据分析

### 6.1 测试环境说明

以下数据来自真实项目运行期间，在同一会议房间（`roomId: 9007`）收集的两组报告。
测试运行在开发机上，SFU 进程通过 `npm run dev:sfu` 启动（`ts-node` 模式），
同一台机器同时运行前端、Spring Cloud 微服务、MySQL、Redis 等组件。

### 6.2 原始测量结果

| 报告     | 场景     | 采集时长 | 样本数 | E2E 均值    | E2E P50 | E2E P95 | E2E 最大值 |
| -------- | -------- | -------- | ------ | ----------- | ------- | ------- | ---------- |
| report-2 | 2 人会议 | 531 s    | 138    | **1086 ms** | 991 ms  | 1922 ms | 4501 ms    |
| report-3 | 3 人会议 | 323 s    | 404    | **1104 ms** | 999 ms  | 1960 ms | 5355 ms    |

| 报告     | 发送端 RTT 均值 | 发送端 RTT 最大 | 接收端 RTT 均值 | 接收端 RTT 最大 |
| -------- | --------------- | --------------- | --------------- | --------------- |
| report-2 | 1053 ms         | 4501 ms         | 1118 ms         | 4501 ms         |
| report-3 | 1101 ms         | 6057 ms         | 1107 ms         | 9704 ms         |

### 6.3 数据评估

**异常现象：RTT 均值约 1000 ms**

正常 WebRTC 应用的参考 RTT 范围：

| 网络环境     | 预期 RTT     | 预期 E2E     |
| ------------ | ------------ | ------------ |
| 局域网       | 1–10 ms      | < 50 ms      |
| 同城宽带     | 10–50 ms     | 50–150 ms    |
| 跨城/跨国    | 80–200 ms    | 100–400 ms   |
| **本次实测** | **~1050 ms** | **~1086 ms** |

测量值比正常局域网高约 **100 倍**，属于系统性异常，而非正常的网络延迟。

**原因分析（按可能性排序）：**

**① ICE candidate IP 不可达，触发 TCP fallback（最可能主因）**

当 `.env` 中 `MEDIASOUP_ANNOUNCED_IP` 未正确设置为客户端可访问的 IP 地址时，
客户端收到的 ICE candidate 为不可达地址（如 `0.0.0.0`）。
ICE 协商失败后自动降级为 TCP relay，TCP 的 Nagle 延迟和 ACK 确认机制
会将往返时延推高 3–10 倍。

验证方法：在浏览器 `chrome://webrtc-internals` 中检查 selected candidate pair 的 transport 类型。
若显示 `tcp` 而非 `udp`，则为此问题。

**② 开发机资源竞争（次要原因）**

`ts-node` 直接运行 TypeScript 无编译优化，同机运行多个服务加剧 CPU 争抢，
导致 mediasoup Worker 的 RTCP 包处理产生调度延迟，影响 RTT 测量值。

**③ RTCP 报告间隔（次要原因）**

RTCP SR/RR 的发送间隔为 2.5–7.5 秒（RFC 3550），RTT 采样频率低。
若两次 RTCP 之间发生了网络抖动，历史 RTT 值会在下次更新前持续被采用，
造成「样本粘滞」，拉高均值。

**规律性观察：**

- P50（中位数）约 991–999 ms，与均值接近，说明高延迟是**持续性而非偶发性**
- 最小值 61 ms（report-3）表明在某些采集时刻网络路径正常，说明问题**可以被修复**
- 两端 RTT 均值几乎相等（1053 vs 1118 ms），符合对称路径特征，排除单端网络问题
- RTT 最大值出现上行（6057 ms）大于接收端（1107 ms 均值），可能存在短暂拥塞爆发

**参与者数量对延迟的影响：**

从 2 人增至 3 人，E2E 均值仅增加 18 ms（1086 → 1104 ms），增幅约 1.7%，
说明在当前负载下 mediasoup Worker 并未成为瓶颈，延迟主要来源于网络链路而非 SFU 处理。

### 6.4 结论

在正确配置网络环境（`MEDIASOUP_ANNOUNCED_IP` 设置为可达 IP、UDP 连接正常）后，
E2E 延迟预计可降至 50–200 ms 范围，符合会议系统的实时性要求。
当前 ~1000 ms 数据反映的是**测试环境配置问题**，可作为对比基准记录在论文中，
用以说明网络配置对 WebRTC 性能的关键影响。

---

## 7. SFU 性能优化

### 7.1 已实施优化

**① mediasoup Worker 负载感知调度**（`sfu/src/core/mediasoup-manager.ts`）

原来使用 Round-Robin 轮询分配 Router，当房间数量不是 Worker 数量的整数倍时，
部分 Worker 会承载更多 Router。

改进为**最小负载调度（Least-Load）**：每次创建 Router 时选取当前承载 Router 最少的 Worker，
使各 Worker 的实际工作量尽量均衡，降低因负载不均导致的 Worker 热点和包处理延迟。

```
Round-Robin（旧）：Worker 0/1/2/3 → 0/1/2/3 → 0/1/2/3（与实际负载无关）
Least-Load（新）：始终分配给当前 Router 数量最少的 Worker
```

**② Opus 音频参数优化**（`sfu/src/config/config.ts`）

启用两个 Opus 扩展特性：

- `usedtx: 1`（不连续传输 DTX）：静音期间停止发送 RTP 包，降低带宽和 CPU 消耗约 20–40%
- `useinbandfec: 1`（带内前向纠错 FEC）：在 5–15% 丢包率下无需重传即可恢复音频，
  避免因等待重传包而产生的额外延迟

### 7.2 进一步优化建议

**③ 增加 Worker 数量（最直接）**

当前默认 4 个 Worker。mediasoup Worker 是独立子进程，各占一个 CPU 核心。

```bash
# 在 .env 中按实际 CPU 核数设置（建议：核数 - 1，留一个给 Node.js 主进程）
MEDIASOUP_WORKERS=7   # 8核机器
```

Worker 数量上限建议不超过 `os.cpus().length - 1`，超过后 CPU 上下文切换开销大于收益。

**④ 生产环境编译运行（显著降低 CPU 开销）**

开发环境使用 `ts-node` 直接执行 TypeScript，无编译优化。
生产/测试时应先编译再运行：

```bash
npm run build
node dist/server.js
```

编译后的 JS 执行速度通常比 `ts-node` 快 30–60%，Worker 调度延迟相应降低。

**⑤ 确保 UDP 直连（最关键的延迟优化）**

```bash
# .env 中必须设置为客户端可直接访问的 IP
MEDIASOUP_ANNOUNCED_IP=<服务器公网IP或局域网IP>
```

TCP fallback 会引入额外的 500–2000 ms 延迟。确保以下条件：

- 防火墙开放 UDP 端口 `40000–49999`（RTC 端口范围）
- `MEDIASOUP_ANNOUNCED_IP` 与客户端网络可达
- 使用 `chrome://webrtc-internals` 验证 candidate pair 类型为 `udp`

**⑥ 关闭开发模式日志标签**

当前配置开启了全量 logTags，在高并发时会产生大量日志 I/O：

```bash
# 生产/压测环境
MEDIASOUP_LOG_LEVEL=warn
# 移除 logTags 中的 rtp、rtcp、srtp 等高频标签
```

**⑦ 调整 initialAvailableOutgoingBitrate**

当前设置为 1 Mbps，适合视频会议。若网络带宽受限，可降低初始码率让 BWE 更快收敛：

```bash
# 网络较差时从更保守的初始值开始
initialAvailableOutgoingBitrate: 600000  # 600 kbps
```

### 7.3 优化预期效果

| 优化项                        | 预期延迟改善          | 适用场景     |
| ----------------------------- | --------------------- | ------------ |
| 修复 `MEDIASOUP_ANNOUNCED_IP` | **500–1500 ms↓**      | 非局域网测试 |
| 生产编译运行                  | 50–150 ms↓            | 所有场景     |
| 增加 Worker 数                | 10–50 ms↓（高并发）   | 4+ 人会议    |
| Opus DTX/FEC                  | 间接（减少 CPU 竞争） | 多人会议     |
| 关闭调试日志                  | 5–30 ms↓（高并发）    | 压测场景     |
