# mediasoup libmediasoupclient Android 路线文档

## 1. 目标

本项目的 Android 端采用 **libmediasoupclient + 自建 JNI/封装** 的路线，避免依赖已久未维护的第三方 Android wrapper。

目标是让 Android 端具备以下能力：
- 作为 SFU 会议房间中的真实 mediasoup peer
- 连接 mediasoup Router
- 创建 send / recv WebRTC transport
- 生产本地音视频流
- 订阅并播放远端媒体流
- 支持后续扩展屏幕共享、字幕、录制等能力

## 2. 当前仓库版本基线

### Android
- Gradle: `8.13`
- Android Gradle Plugin: `8.13.2`
- Kotlin: `2.0.21`
- compileSdk: `36`
- minSdk: `26`
- Java source/target: `11`
- Gradle 运行 JDK: `17`

### SFU
- mediasoup: `^3.19.12`
- mediasoup-client: `^3.18.1`
- Node.js SFU 通过 `socket.io` 提供房间信令

### 官方 libmediasoupclient 基线
- `libmediasoupclient` 采用 upstream `v3` 线路
- 当前官方支持的 libwebrtc 基线为 `m140` / `branch-heads/7339`
- 官方仓库没有稳定 Maven/Gradle 发行包，必须本地构建并用 commit SHA 锁定

## 3. 推荐目录结构

建议在 Android 模块内新增原生代码边界：

```text
android/
  app/
    src/main/cpp/
      CMakeLists.txt
      bridge/
        SfuNativeBridge.cpp
        SfuNativeBridge.h
      jni/
        SfuJni.cpp
        SfuJni.h
    src/main/java/com/theoyu/thesis/android/core/sfu/
      SfuMediaEngine.kt
      SfuSession.kt
      SfuTransport.kt
      SfuProducer.kt
      SfuConsumer.kt
```

推荐原则：
- Kotlin 只做业务编排和 UI 状态管理
- JNI 层只做 Java/Kotlin 与 C++ 对接
- libmediasoupclient、libwebrtc、JNI 逻辑全部隔离在 native 边界
- 不要把 WebRTC 细节散落在 `ViewModel` 中

## 4. 组件版本矩阵

### 必须固定的版本
- Android Gradle Plugin: `8.13.2`
- Gradle: `8.13`
- JDK: `17`
- Kotlin: `2.0.21`
- compileSdk: `36`
- minSdk: `26`
- ABI 首选：`arm64-v8a`
- 第二目标：`x86_64`

### Native 侧建议版本
- libmediasoupclient: upstream `v3`，按 commit SHA 锁定
- libwebrtc: `m140` / `branch-heads/7339`
- CMake: Android Studio SDK Manager 安装的稳定版
- NDK: Android Studio SDK Manager 安装的稳定版

## 5. 构建环境

### 开发机要求
- Android Studio 最新稳定版
- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.x
- Android NDK
- CMake
- Ninja（通常随 CMake/NDK 体系一起使用）

### 终端和构建工具
- Windows / macOS / Linux 均可
- 必须能执行：
  - `./gradlew :app:assembleDebug`
  - `gn`
  - `ninja`
  - `cmake`

### Android Studio 配置
安装以下组件：
- Android SDK Platform 36
- Android SDK Build Tools 36.x
- Android NDK
- CMake

Gradle 外部原生构建通过 `externalNativeBuild` 接入。

## 6. 构建链步骤

### Step 1: 拉取 libwebrtc
按照 mediasoup 官方文档构建 `m140` 分支对应的 libwebrtc。

建议：
- 单独保存构建脚本
- 记录 GN args
- 记录 libwebrtc commit / branch-head

### Step 2: 构建 libmediasoupclient
按照官方 `libmediasoupclient` 文档，用 Step 1 产物编译 C++ client 库。

关键点：
- 对齐 `m140` libwebrtc
- 不要混用不同 WebRTC 版本
- 构建结果要固定到可复现的输出目录

### Step 3: 构建 JNI Bridge
JNI 层负责：
- 初始化 native client
- 暴露 `device.load()`
- 创建 send / recv transport
- 处理 `connectTransport`
- 暴露 `produce` / `consume` / `close`

### Step 4: 接入 Android App
在 Kotlin 层提供 `SfuMediaEngine`：
- `initialize()`
- `joinRoom()`
- `loadRouterCapabilities()`
- `createTransport()`
- `produceTrack()`
- `consumeTrack()`
- `closeRoom()`

## 7. 信令契约

Android 端继续复用当前 `socket.io` 协议，建议维持以下顺序：

1. `joinRoom`
2. `getRouterRtpCapabilities`
3. `createWebRtcTransport` (send)
4. `createWebRtcTransport` (recv)
5. `connectWebRtcTransport`
6. `produce`
7. `consume`
8. `resumeConsumer`

必须遵守：
- send / recv transport 分离
- 生产端必须先完成 `connectWebRtcTransport`
- 消费端需要 Router RTP capabilities
- 本地 producer 和远端 consumer 都要可追踪到 roomId / peerId / transportId

## 8. JNI 设计约束

### 必须声明的接口
- `nativeInit()`
- `nativeLoadDevice(routerRtpCapabilitiesJson)`
- `nativeCreateSendTransport(...)`
- `nativeCreateRecvTransport(...)`
- `nativeConnectTransport(...)`
- `nativeProduce(...)`
- `nativeConsume(...)`
- `nativeClose()`

### 生命周期约束
- Activity / ViewModel 销毁时必须释放 native 资源
- 离开房间时必须 close transport、producer、consumer
- 重连时需要重建 session，而不是复用过期对象

### 线程约束
- 网络回调和 native 回调不要直接阻塞主线程
- JNI 回调到 Kotlin 后再切回 `viewModelScope`

## 9. 编解码与能力约束

建议先支持：
- 音频：`opus`
- 视频：`VP8`

后续再扩展：
- `H264`
- 屏幕共享单独 video producer
- Simulcast / SVC

注意：
- Router 端 codec 必须与 native 客户端能力匹配
- 生产者和消费者的 RTP 参数必须由 mediasoup 体系生成，不可手写伪造

## 10. 当前项目的实现边界

当前仓库已经有：
- 房间会议 UI
- `socket.io` 信令
- `joinRoom` / `createWebRtcTransport` / `consume` 等服务端接口

当前仍缺：
- libwebrtc native 构建
- libmediasoupclient native 构建
- JNI wrapper
- 真实本地音视频采集与渲染

因此本路线文档的目标是：
- 先把 native 构建链规范化
- 再把 Android 房间逻辑和 native 媒体能力接起来

## 11. 验收标准

完成后应至少满足：
- Android 能正常编译 native 库
- 能通过 `joinRoom` 进入 SFU 房间
- 能创建 send / recv transport
- 能触发本地 audio/video producer
- 能消费并渲染远端 producer
- 能在断线重连后重建会话
- 能在 Host 场景下执行 mute / remove / close room

## 12. 维护建议

- libwebrtc 版本必须单独记录
- libmediasoupclient commit 必须单独记录
- JNI ABI 输出必须按 `arm64-v8a` / `x86_64` 分目录
- 每次升级 Android Gradle Plugin 或 NDK，都要重新验证 native 构建
- 每次升级 mediasoup server 版本，都要复查 RTP capabilities / codec 列表

## 13. 参考资料

- https://mediasoup.org/documentation/v3/libmediasoupclient/
- https://mediasoup.org/documentation/v3/libmediasoupclient/installation/
- https://mediasoup.org/documentation/v3/communication-between-client-and-server/
- https://mediasoup.org/documentation/v3/mediasoup/api/
- https://developer.android.com/studio/projects/install-ndk
- https://developer.android.com/studio/projects/gradle-external-native-builds
- https://developer.android.com/build/jdks

