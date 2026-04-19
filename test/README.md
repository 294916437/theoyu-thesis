# 系统测试

## 功能验证

## SFU流媒体并发与延迟

### 并发压力测试

见 `concurrent-stress/concurrent-stress.js`，通过脚本模拟多路 Broadcaster 并发接入，采集 CPU/内存/连接数等指标。

### 端到端延迟测试

基于真实浏览器的 E2E 延迟测量，测试文件位于 `e2e-latency/`。

#### 原理

| 角色                        | 动作                                                                                                                                                            |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 发送端 (`sender-main.html`) | 用 Canvas 每帧绘制当前毫秒时间戳，通过 `captureStream(30)` 发布至 SFU；同时通过 `BroadcastChannel` 将时间戳广播给同浏览器其他标签页                             |
| 接收端 (`receiver.html`)    | 订阅发送端视频流，通过 `BroadcastChannel` 接收最新发送时间戳；每 200ms 记录一次 `Date.now() - lastSentTs` 作为 E2E 延迟样本；采集满 60 次后自动汇总均值和最大值 |

> **为何用 BroadcastChannel 而非视频帧 OCR？**  
> 在同浏览器多标签测试场景下，BroadcastChannel 能直接传递精确时间戳，避免 Canvas 截图 + 字符识别引入的误差（>10ms），精度更高、实现更简洁。

#### 测试步骤

**环境准备**

```bash
# 启动 SFU（测试模式，跳过 gRPC 鉴权）
cd sfu
SFU_TEST_MODE=true npm run dev   # Linux/macOS
# Windows PowerShell:
$env:SFU_TEST_MODE="true"; npm run dev
```

**2 人规模测试**

1. 浏览器标签页 A：打开 `sfu/e2e-latency/sender-main.html`
   - 填写房间 ID（如 `e2e-test-room`）
   - 点击「开始推流」
2. 浏览器标签页 B：打开 `sfu/e2e-latency/receiver.html`（**必须同一浏览器窗口，BroadcastChannel 同源**）
   - 填写相同房间 ID，「参与者规模」填 `2`
   - 点击「加入房间并开始采样」
3. 等待 60 次采样完成，页面自动显示均值和最大值
4. 点击「导出 JSON」保存原始数据

**3 人 / 5 人规模测试**

- 额外打开对应数量的 `sender-main.html` 标签页（不同 userId），分别加入同一房间
- 接收端「参与者规模」对应修改为 `3` / `5`，重新采样

**RTCP RTT 数据采集**

在任意参与者标签页地址栏输入 `chrome://webrtc-internals`，展开对应的 PeerConnection，查看 **RTCIceCandidatePair** 下的 `currentRoundTripTime`（单位秒，×1000 转为 ms）。

记录方式：在稳定推流期间，截图或手动读取 3~5 个时刻的 RTT 值取均值。

#### 结果表格

测试完成后，将各规模数据汇总如下：

| 参与者数 | E2E延迟均值 (ms) | E2E延迟最大值 (ms) | RTCP RTT均值 (ms) |
| :------: | :--------------: | :----------------: | :---------------: |
|    2     |        —         |         —          |         —         |
|    3     |        —         |         —          |         —         |
|    5     |        —         |         —          |         —         |

> 各项均为 60 次有效采样的统计值（异常值过滤：延迟 < 0 或 > 5000ms 的样本丢弃）。

## 端侧视频处理性能对比

## 微服务接口压测
