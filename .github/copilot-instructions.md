# 项目介绍

1. 题目：基于 WebRTC 的实时视频会议系统设计与优化
2. 技术栈
   - **WebRTC&Mediasoup**: 媒体通信框架
   - **Rust**: 编译为 wasm 模块，通过 WebAssembly 实现端侧的视频流处理
   - **前端**: Vue.js + Vuetify + Pinia + Axios + Pixi.js + Vite 技术栈
   - **后端 SpringCloud**: 微服务架构，SprintBoot+MySQL+Redis+RocketMQ+Nacos+Minio+Cassandra+gRPC
   - **后端 SFU**: Node.js (Socket.io 和 SFU 服务)，Express+Mediasoup+Socket.io+gRPC
3. 研究重点
   - 基于**SFU架构**的多人实时音视频会议，支持多人会议的实时媒体流的接收与转发
   - 基于**Rust+Wasm**的端侧视频流处理与优化，包括背景模糊与背景替换
   - 基于**WebRTC**实现 P2P 的音视频通话
   - 异构微服务架构，业务逻辑层使用**Spring Cloud Alibaba**，流媒体服务层使用**Node.js (Express) + Mediasoup**构建SFU服务器，两者通过**gRPC**进行通信
4. 功能模块
   - 用户管理：基于鉴权方案实现统一认证，支持用户注册、登录、Profile展示与编辑、。
   - 会议室管理：支持创建会议(分为立即会议和预约会议两种类型)、加入会议。主持人可对普通成员执行静音、移出房间、全体静音、关闭全体摄像头等权限控制。
   - 流媒体通信：基于SFU架构，实现多路媒体流的转发与订阅，支持带宽自适应调整。
   - 端侧视频处理：基于Rust编写的WebAssembly模块，在浏览器端实现基于视频背景分割的背景模糊与背景替换。
   - 会议功能扩展：支持会议录制与回放及持久化存储、支持屏幕共享功能。
   - 会议房间聊天：支持房间内成员的在线文字聊天和文件分享。
   - 平台私聊中心：支持私人模式下的实时聊天和音视频文件分享以及P2P的音视频交流

# 代码规范

# 架构设计

## 总体架构设计

## 异构微服务设计

系统采用**异构微服务架构**，纵向分为四层：

```

┌─────────────────────────────────────────────────────────────────┐
│ 接入层 (Gateway Layer) │
│ thesis-gateway :8000 · Spring Cloud Gateway │
│ 职责：路由转发 / Sa-Token 统一鉴权 / userId 透传 / 文件限流 │
│ 路由：/auth/** /user/** /message/** /media/** /file/\*\* │
└─────────────────────────┬───────────────────────────────────────┘
│ lb:// 负载均衡（Nacos 服务发现）
┌─────────────────────────▼───────────────────────────────────────┐
│ 业务层 (Business Layer) │
│ thesis-auth :8001 认证授权、验证码 │
│ thesis-user :8002 用户信息、在线状态、头像上传 │
│ thesis-chat :8003 私聊会话、消息存储、P2P WebRTC 信令 │
│ thesis-media :8086 会议室管理、录制控制、gRPC 双向通信 │
│ thesis-oss :8081 文件/对象存储（Minio 封装） │
│ id-generator :8085 分布式雪花ID生成 │
│ thesis-kv :8084 Redis KV 统一封装 │
│ 服务间通信：Nacos 服务发现 + RocketMQ 异步消息 + Redis 缓存共享│
└─────────────────────────┬───────────────────────────────────────┘
│ gRPC (双向 :50051 / :50052)
┌─────────────────────────▼───────────────────────────────────────┐
│ 媒体层 (Media/SFU Layer) │
│ Node.js SFU 服务 :3000 · Express + Mediasoup + Socket.io │
│ 职责：WebRTC 媒体流 SFU 转发、会议信令、录制管理、媒体统计 │
│ 与客户端：Socket.io 信令 + WebRTC UDP/TCP 媒体流 │
│ 与业务层：gRPC 双向通信（房间验证/用户鉴权/事件回调/统计上报） │
└─────────────────────────┬───────────────────────────────────────┘
│
┌─────────────────────────▼───────────────────────────────────────┐
│ 存储层 (Storage Layer) │
│ MySQL 8.0 :3306 关系型数据（用户/会议/消息/会话） │
│ Redis 7.2 :6379 缓存、Token 会话、在线状态 │
│ Cassandra 5 :9042 时序数据、媒体统计、流量指标 │
│ Minio :9000 对象存储（用户头像/上传文件/录制视频） │
│ RocketMQ :9876 异步消息（缓存失效/事件解耦） │
│ Nacos v2.2.3 :8848 服务注册发现 + 配置中心（双重用途） │
└─────────────────────────────────────────────────────────────────┘

```

**前端通信方式**：

- HTTP REST：axios 统一封装（`frontend/src/api/`），所有请求经 Vite 代理 `/api` → SpringCloud Gateway `:8000`
- WebSocket：Socket.io 直连 SFU `:3000`（媒体信令、信令服务），Stomp 经网关到 `thesis-chat`（WebRTC P2P信令服务） 和 `thesis-media`（会议内聊天和会议事件）
- 排除鉴权路径：`/auth/login`、`/auth/verification/code/send`、WebSocket 升级接口

**各微服务职责边界**：

```

| 服务                | 端口 | 职责                                                      |
| ------------------- | ---- | --------------------------------------------------------- |
| thesis-gateway      | 8000 | 统一接入、Sa-Token 鉴权、路由转发、userId 注入下游 Header |
| thesis-auth         | 8001 | 登录/注册、验证码发送、Token 颁发（Sa-Token）             |
| thesis-user         | 8002 | 用户 Profile 读写、头像上传（调用 OSS）、在线状态管理     |
| thesis-chat         | 8003 | 私聊会话管理、消息持久化（MySQL）、WebRTC P2P 信令转发    |
| thesis-media        | 8086 | 会议室 CRUD、录制控制；gRPC 桥接 SFU 与业务层             |
| thesis-oss          | 8081 | Minio 对象存储封装，供其他服务 RPC 调用                   |
| thesis-id-generator | —    | 雪花算法分布式 ID，供其他服务 RPC 调用                    |
| thesis-kv           | 8084 | KV 操作封装，供其他服务 RPC 调用                          |

```

**Nacos 用途**：**同时**承担服务注册发现与配置中心；各服务均通过 `namespace: thesis` + `group: DEFAULT_GROUP` 从 Nacos 拉取配置，并在 Nacos 注册服务实例供网关负载均衡。

## 音视频信令与媒体流路由设计

## 数据库与存储

### MySQL 设计

```

```
