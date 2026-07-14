# Android 项目初始化指南

本文档用于约束 Android 端从零初始化时的仓库位置、技术选型和与现有 Web/SFU/后端系统的集成边界。Android Studio 生成的 Gradle 工程文件由你手动创建，本仓库只维护通用说明与 monorepo 级配置。

## 目标

Android 端不是复用 Web UI，而是复用已有后端能力：

- 业务接口复用 Spring Cloud Gateway 暴露的 REST API。
- 多人会议媒体链路对接现有 Node.js + Mediasoup SFU。
- 私聊 P2P 音视频沿用 WebRTC 信令模型，但 Android 端需要用原生 WebRTC SDK 实现采集、编码、PeerConnection 和媒体渲染。
- UI 与业务状态使用 Kotlin、Jetpack Compose、Material 3、ViewModel、StateFlow 重新实现。

## 使用 Android Studio 创建工程

在仓库根目录执行以下人工操作：

1. 打开 Android Studio，选择 `New Project`。
2. 选择 `Empty Activity`，并启用 Jetpack Compose。
3. Project location 选择 `D:\git\monorepo\theoyu-thesis\android`。
4. Package name 建议使用 `com.theoyu.thesis.mobile`。
5. Minimum SDK 建议选择 API 26 或更高。
6. Language 选择 Kotlin。
7. 等待 Android Studio 自动生成 Gradle wrapper、`settings.gradle.kts`、`build.gradle.kts`、`app/` 等文件。

初始化后，Android 工程应保持在 `android/` 目录内，不要把 Gradle wrapper 放到 monorepo 根目录，避免和 Node、Maven、Rust 子项目混淆。

## 推荐模块结构

```text
android/
├── app/
│   └── src/main/java/com/theoyu/thesis/mobile/
│       ├── core/                 # 网络、WebRTC、Socket、权限、日志等基础能力
│       ├── data/                 # REST DTO、Repository、远端数据源
│       ├── domain/               # 会议、用户、聊天等业务模型与用例
│       ├── feature/
│       │   ├── auth/             # 登录、注册、鉴权状态
│       │   ├── meeting/          # 会议列表、详情、预约、加入
│       │   ├── room/             # 多人会议房间 UI 与控制
│       │   └── chat/             # 房间聊天与私聊
│       └── ui/                   # Theme、通用 Compose 组件
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

## WebRTC 与 Mediasoup 接入建议

Android 端不要把 Web 端的 `mediasoup-client` 代码直接迁移。推荐分层：

- `SignalingClient`：负责 Socket.io 连接、房间加入、生产者/消费者事件、主持人控制事件。
- `WebRtcGateway`：负责摄像头/麦克风采集、PeerConnectionFactory、音视频 Track、Renderer、设备切换、释放资源。
- `MeetingRepository`：负责创建会议、加入会议、会议列表、会议成员、录制回放等 REST API。
- `RoomViewModel`：组合 REST 状态、Socket 状态和媒体状态，向 Compose 暴露不可变 UI state。

依赖选择需要单独验证：

- WebRTC Android SDK 可先调研 `org.webrtc:google-webrtc`、Jitsi WebRTC 预构建包或项目自编译 libwebrtc。
- Mediasoup Android 客户端需要确认当前维护状态、与服务端 Mediasoup 版本兼容性、AAR 发布源和 ProGuard/R8 配置。
- Socket.io Android 客户端需要与现有 SFU 服务端 Socket.io 版本做握手兼容测试。

首个里程碑建议只完成权限、登录态、会议列表、创建/加入会议和 Socket 连通性；第二个里程碑再接入本地摄像头预览和 SFU 发布/订阅。

## Manifest 权限

多人会议 App 至少需要：

- `android.permission.INTERNET`
- `android.permission.CAMERA`
- `android.permission.RECORD_AUDIO`
- `android.permission.MODIFY_AUDIO_SETTINGS`

运行时权限必须在进入会议前申请，并处理拒绝、取消和再次申请。

## 与现有仓库的约定

- Android 构建产物、Gradle 缓存、签名密钥和 `local.properties` 已在根 `.gitignore` 中忽略。
- 根 `package.json` 只管理 Node workspace，不把 Android 加入 npm workspaces。
- Android 端配置、依赖版本和 wrapper 由 Android Studio 生成并维护在 `android/` 内。
- 后续若需要 CI，可单独增加 Android workflow，不复用 web/sfu 的 npm 构建命令。
