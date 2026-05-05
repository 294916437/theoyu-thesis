# 系统测试

## 功能验证

## SFU流媒体并发与延迟

### 并发压力测试

见 `concurrent-stress/concurrent-stress.js`，通过脚本模拟多路 Broadcaster 并发接入，采集 CPU/内存/连接数等指标。

### 端到端延迟测试

基于真实浏览器的 E2E 延迟测量，测试文件位于 `e2e-latency/`。

#### 测量原理

| 角色                        | 动作                                                                                                                                                                                              |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 发送端 (`sender-main.html`) | 通过 `getUserMedia` 获取真实摄像头流，每帧将摄像头画面绘入 Canvas（640×480），并在右下角叠加 QR 码（内容为当前 `Date.now()` 的 13 位 Unix 毫秒时间戳），通过 `captureStream(30)` 发布至 SFU       |
| 背景参与者 (`sender.html`)  | 通过 `getUserMedia` 获取真实摄像头流，直接发布至 SFU，同时订阅其他参与者流，模拟真实 N 路会议的转发负载                                                                                           |
| 接收端 (`receiver.html`)    | 通过 `requestVideoFrameCallback` 获取帧展示时刻 `now`（`performance.now()`）；裁剪视频帧右下角 QR 码区域后调用 **jsQR** 同步解码得 `sentTs`；E2E 延迟 = `(now + performance.timeOrigin) − sentTs` |
| RTT 采集                    | 接收端每 10 次采样调用 `recvTransport.getStats()`，从 `candidate-pair` stats 中读取 `currentRoundTripTime`（×1000 转 ms）                                                                         |

> **为何用 jsQR 而非 BarcodeDetector？**  
> `BarcodeDetector` API 在 Chrome 中属于实验性功能，在 Windows 上部分版本不可用（Chromium bug #1042132，Shape Detection API 在 Windows 默认禁用）。jsQR 是纯 JavaScript 实现，无需任何浏览器内置 API，兼容所有现代浏览器，且对裁剪后的 236×236px 图像解码速度约 2–5ms，不影响测量精度。

#### 环境要求

- **浏览器**：Chrome / Edge（推荐）；Firefox 亦可
  - `requestVideoFrameCallback` 需 Chrome 83+ / Edge 83+ / Firefox 132+；若浏览器不支持，可临时用 `requestAnimationFrame` 替代（精度略降）
- **摄像头**：所有标签页均需授予摄像头权限（`sender-main.html` 和 `sender.html` 均调用 `getUserMedia`）
- **SFU 服务**：启动时设置 `SFU_TEST_MODE=true` 以跳过 gRPC 鉴权

```bash
cd sfu
# Windows PowerShell:
$env:SFU_TEST_MODE="true"; npm run dev
# Linux/macOS:
SFU_TEST_MODE=true npm run dev
```

#### 完整测试步骤

> 所有标签页须在**同一台机器的同一浏览器窗口**中打开，共用同一 SFU 本地实例。  
> 每组测试前先点击上一组的「⏹ 停止」，等待 SFU 清理房间（约 5s），再开新一轮。  
> 打开各标签页时，浏览器会弹出摄像头权限请求，点击**允许**后再点击推流按钮。

---

**2 人规模**（发送端 1 + 接收端 1）

| 标签页 | 文件               | 关键参数                                              |
| ------ | ------------------ | ----------------------------------------------------- |
| A      | `sender-main.html` | userId=`sender-001`，房间=`e2e-room-2p`               |
| B      | `receiver.html`    | userId=`receiver-001`，房间=`e2e-room-2p`，规模=**2** |

操作顺序：

1. 打开标签页 A，授予摄像头权限，点击「🚀 开始推流」，等待状态显示「真实摄像头推流中」
2. 打开标签页 B，填入相同房间 ID，「参与者规模」填 `2`，点击「🚀 加入房间并开始采样」
3. 状态栏显示 `✅ jsQR 已初始化` → `开始采样` → 采样计数开始递增
4. 等待 60 次采样完成，页面自动显示 E2E 均值、最大值、RTCP RTT 均值
5. 点击「💾 导出 JSON」，保存文件 `e2e-latency-2p-<timestamp>.json`

---

**4 人规模**（发送端 1 + 背景参与者 2 + 接收端 1）

| 标签页 | 文件               | 关键参数                                              |
| ------ | ------------------ | ----------------------------------------------------- |
| A      | `sender-main.html` | userId=`sender-001`，房间=`e2e-room-4p`               |
| B      | `sender.html`      | userId=`sender-002`，房间=`e2e-room-4p`               |
| C      | `sender.html`      | userId=`sender-003`，房间=`e2e-room-4p`               |
| D      | `receiver.html`    | userId=`receiver-001`，房间=`e2e-room-4p`，规模=**4** |

操作顺序：

1. 打开标签页 A，授予摄像头权限，点击「🚀 开始推流」（带 QR 码时间戳的主发送端）
2. 打开标签页 B、C，各自授予摄像头权限，点击「🚀 加入并推流」（真实摄像头，制造 SFU 转发负载）
3. 打开标签页 D，「参与者规模」填 `4`，点击「🚀 加入房间并开始采样」
4. 等待 60 次采样完成，导出 JSON

---

**6 人规模**（发送端 1 + 背景参与者 4 + 接收端 1）

| 标签页 | 文件               | 关键参数                                              |
| ------ | ------------------ | ----------------------------------------------------- |
| A      | `sender-main.html` | userId=`sender-001`，房间=`e2e-room-6p`               |
| B      | `sender.html`      | userId=`sender-002`，房间=`e2e-room-6p`               |
| C      | `sender.html`      | userId=`sender-003`，房间=`e2e-room-6p`               |
| D      | `sender.html`      | userId=`sender-004`，房间=`e2e-room-6p`               |
| E      | `sender.html`      | userId=`sender-005`，房间=`e2e-room-6p`               |
| F      | `receiver.html`    | userId=`receiver-001`，房间=`e2e-room-6p`，规模=**6** |

操作顺序：依次打开标签页 A→E，各授予摄像头权限并推流（各自填不同 userId），最后打开标签页 F 开始采样，等待 60 次完成后导出。

---

**注意事项**

- **摄像头权限**：浏览器须对每个标签页单独授权，若弹窗被拦截，在地址栏右侧图标中手动允许
- **QR 码解码失败**：若 `raw-data` 区域长时间无数据，检查视频是否正常播放（remote-video 元素应有画面），可尝试刷新接收端页面重新加入
- **延迟为负数被过滤**：说明接收端时钟快于发送端（本地回环不会出现，若跨机器需注意 NTP 同步）
- **RTCP RTT 显示 `—`**：说明 `candidate-pair` stats 中尚无 `nominated` 对，此为正常现象（ICE 协商中），通常在推流稳定 10s 后可采集到

#### 结果汇总表

将三组导出 JSON 中的数据填入下表（字段对应 `avgLatencyMs`、`maxLatencyMs`、`avgRttMs`）：

| 参与者数 | 采样次数 | E2E 延迟均值 (ms) | E2E 延迟最大值 (ms) | RTCP RTT 均值 (ms) |
| :------: | :------: | :---------------: | :-----------------: | :----------------: |
|    2     |    60    |         —         |          —          |         —          |
|    4     |    60    |         —         |          —          |         —          |
|    6     |    60    |         —         |          —          |         —          |

> 各项均为 60 次有效采样的统计值；过滤规则：延迟 ≤ 0 或 ≥ 5000ms 的样本丢弃不计。

## 端侧视频处理性能对比

### wasm对比js

<!--
以下为论文正文章节内容
-->

---

## 6.3.2 不同背景效果的性能对比

### 测试方案

本节在真实摄像头视频流场景下，对无效果（None）、背景模糊（Blur）和背景替换（Replace）三种模式的端侧帧处理性能进行横向对比，测试分辨率固定为640×480，目标帧率为30fps，对应帧预算33.33ms。

测试按模式顺序串行执行，每种模式先以30帧热身以保证V8 JIT充分优化，随后连续采集120帧。计时边界的界定如下：None模式仅包裹`drawImage(video)`一次调用，作为纯渲染基线；Blur和Replace模式则以`getImageData()`前为起点、`putImageData()`后为终点，涵盖Wasm内存写入、`prepare_mask()`、渲染函数调用及输出帧回读的完整链路。ONNX推理运行于独立的异步循环，不纳入帧耗时统计。captureStream(30)实际帧率借助`requestVideoFrameCallback`逐帧计数后换算，主线程长任务（≥50ms）由`PerformanceObserver`按模式独立累计。

### 测试结果

表6-X汇总了三种模式下的核心性能指标。

**表6-X 三种背景效果模式性能对比（640×480，各采集120帧）**

| 指标                         | 无效果（None） | 背景模糊（Blur） | 背景替换（Replace） |
| :--------------------------- | -------------: | ---------------: | ------------------: |
| 均值耗时（ms）               |         4.8792 |          34.5333 |             15.8067 |
| 标准差（ms）                 |         1.9828 |           0.7056 |              0.5003 |
| 中位数（ms）                 |         5.0500 |          34.4000 |             15.8000 |
| P95耗时（ms）                |         8.0000 |          35.6000 |             16.8000 |
| 最大值（ms）                 |        10.9000 |          38.6000 |             17.2000 |
| 帧预算占用率                 |         14.64% |          103.60% |              47.42% |
| captureStream实测帧率（fps） |          27.44 |            17.67 |               25.10 |
| rAF循环帧率（fps）           |         126.64 |            20.38 |               37.64 |
| 主线程长任务次数             |              0 |                0 |                   0 |
| 相对None开销倍数             |         1.000× |           7.078× |              3.240× |

### 结果分析

**（一）无效果基线（None）**

None模式的均值耗时为4.88ms，帧预算占用率仅14.64%，主线程压力极低。rAF循环帧率高达126.64fps，远超目标帧率，说明在无Wasm介入的情况下，浏览器渲染管线本身几乎不构成瓶颈。captureStream实测27.44fps与目标30fps存在小幅差距，属于`captureStream` API自身调度抖动的正常范围，并非帧处理能力不足所致。

相较而言，None模式的标准差（1.98ms）在三者中最大，这与Wasm计算的确定性无关，反映的是浏览器事件循环调度`drawImage`时的背景噪声。

**（二）背景替换（Replace）**

Replace模式的均值耗时为15.81ms，帧预算占用率47.42%，较基线增加10.93ms，相对开销为3.24倍。标准差0.50ms，逐帧耗时高度稳定；P95耗时16.8ms，距33.33ms的帧预算仍留有足够裕量。rAF帧率实测37.64fps，captureStream实测25.10fps，均能维持在可接受范围内。

`render_replace()`的实现属于单次线性扫描：对640×480共307,200个像素依次执行Alpha混合，计算逻辑为

$$\text{Output}[i] = \text{Input}[i] \times \alpha_i + \text{Background}[i] \times (1 - \alpha_i)$$

访存模式严格顺序，无分支，对CPU预取和Wasm线性内存模型均极为友好，因此整体耗时的可控性较强。

**（三）背景模糊（Blur）**

Blur模式的均值耗时为34.53ms，超出帧预算0.20ms，帧预算占用率达103.60%。P95耗时35.6ms同样超标。rAF帧率降至20.38fps，captureStream实测仅17.67fps，相比目标30fps下降幅度分别约32%和41%。

性能瓶颈集中在`render_blur()`中的三轮可分离盒型模糊（Box Blur）。具体实现上，每轮模糊由一次水平前缀和扫描与一次垂直滑动窗口扫描组成，单轮时间复杂度为$O(W \times H)$，三轮叠加以近似高斯模糊效果。在640×480分辨率下，三轮合计约180万像素级操作，内存带宽消耗显著高于Replace模式——结合上节Wasm基准测试中`prepare_mask()`单独耗时约12.92ms，可估算`render_blur()`本身的执行时间约为34.53 − 12.92 = 21.61ms，是整条处理链路中占比最高的环节，也是Blur与Replace之间18.73ms性能差距的直接来源。

尽管如此，Blur模式的标准差仅0.71ms，三种模式中最小，变异系数约为2%。盒型模糊算法的计算量与输入内容无关，每帧执行路径完全固定，因此表现出近乎恒定的执行时间。0次长任务记录也证明了这一点：Blur不存在偶发性帧间卡顿，始终以稳定的"慢"运行。

**（四）综合对比**

三种模式的帧耗时分布特征对比鲜明。None模式耗时低但方差偏大，受浏览器调度影响较明显；Blur和Replace两者则由Wasm计算主导，方差极小，执行行为高度可预测。

从帧率达成情况来看，Replace模式在30fps目标下具备实用价值：rAF帧率37.64fps说明单帧处理时间不会阻塞下一帧的触发，captureStream实测25.10fps的不足主要源于`captureStream` API本身的固有延迟与缓冲机制。Blur模式在当前参数配置（模糊半径15、模糊轮数3）下已超出帧预算，若需满足30fps场景，可从三个方向着手优化：一是降低模糊半径（当前默认值15），二是减少模糊轮数（由3轮改为2轮），三是对输入图像先下采样再模糊后上采样，以缩减盒型模糊的实际操作像素数量。

---

══════════════════════════════════════════════════════════════
Wasm vs Pure JS — prepare_mask() 性能对比报告
分辨率: 640×480 | 热身: 50 帧 | 采集: 100 帧
执行顺序交替 (偶数帧先 Wasm / 奇数帧先 JS)
计时方法: performance.now() 前后打点，仅计入纯计算耗时
──────────────────────────────────────────────────────────────

Rust/Wasm prepare_mask()
Mean : 12.9210 ms
StdDev : 0.9280 ms
Median : 12.7000 ms
Min : 12.1000 ms
Max : 19.5000 ms
P95 : 14.1000 ms

Pure JS prepare_mask()
Mean : 21.5910 ms
StdDev : 1.4105 ms
Median : 21.4000 ms
Min : 20.6000 ms
Max : 34.9000 ms
P95 : 22.2000 ms

★ 加速比 (Speedup) : 1.671× (JS.mean / Wasm.mean)

JS 各步骤平均耗时占比:
computeGrayCache : 0.5780 ms (2.7%)
bilinearUpsample : 1.4070 ms (6.5%)
temporalSmooth : 0.4000 ms (1.9%)
jointBilateralFilter : 19.2060 ms (89.0%)
══════════════════════════════════════════════════════════════

### effect对比

══════════════════════════════════════════════════════════════
Effect Mode Benchmark — 端侧背景效果模式性能对比报告
分辨率: 640×480 | 目标帧率: 30 fps | 帧预算: 33.3333 ms
热身: 30 帧/模式 | 采集: 120 帧/模式
None 计时: drawImage(video)；Blur/Replace 计时: getImageData → putImageData
──────────────────────────────────────────────────────────────

无效果 (None)  
 Mean : 4.8792 ms
StdDev : 1.9828 ms
Median : 5.0500 ms
Min : 1.9000 ms
Max : 10.9000 ms
P95 : 8.0000 ms
帧预算占用 : 14.64 %
captureStream : 27.44 fps (目标 30 fps)
rAF FPS : 126.64 fps
长任务 ≥50ms : 0 次

背景模糊 (Blur)  
 Mean : 34.5333 ms
StdDev : 0.7056 ms
Median : 34.4000 ms
Min : 33.3000 ms
Max : 38.6000 ms
P95 : 35.6000 ms
帧预算占用 : 103.60 %
captureStream : 17.67 fps (目标 30 fps)
rAF FPS : 20.38 fps
长任务 ≥50ms : 0 次

背景替换 (Replace)
Mean : 15.8067 ms
StdDev : 0.5003 ms
Median : 15.8000 ms
Min : 14.7000 ms
Max : 17.2000 ms
P95 : 16.8000 ms
帧预算占用 : 47.42 %
captureStream : 25.10 fps (目标 30 fps)
rAF FPS : 37.64 fps
长任务 ≥50ms : 0 次

──────────────────────────────────────────────────────────────
相对性能对比 (基准: None)
Blur overhead : +29.6542 ms (7.078×)
Replace overhead : +10.9275 ms (3.240×)
Blur vs Replace : 18.7267 ms 差异
Blur rAF FPS drop : 9.62 fps vs 30 fps 目标
Replace FPS drop : -7.64 fps vs 30 fps 目标

══════════════════════════════════════════════════════════════

## 微服务接口压测
