# SFU Server - 基于 Mediasoup 的视频会议服务

## 📋 项目简介

这是一个基于 **Mediasoup v3** 构建的 SFU (Selective Forwarding Unit) 服务器，专门用于多方视频会议系统的媒体流转发。采用 **Node.js + TypeScript** 开发，通过 **gRPC** 与 Spring Cloud 微服务架构集成。

### 核心特性

- ✅ **高性能媒体转发**：基于 Mediasoup 的 SFU 架构，支持多路音视频流
- ✅ **WebRTC 标准支持**：完整的 WebRTC 信令和媒体处理
- ✅ **微服务集成**：通过 gRPC 与 Spring Cloud 无缝对接
- ✅ **服务注册发现**：集成 Nacos 实现服务自动注册
- ✅ **实时通信**：基于 Socket.io 的低延迟信令传输
- ✅ **多编解码器支持**：VP8、VP9、H.264、Opus
- ✅ **生产级可靠性**：完善的错误处理和优雅关闭机制

---

## 🏗️ 技术架构

### 架构分层

```
┌─────────────────────────────────────────────┐
│         客户端层 (WebRTC Client)              │
│  - 浏览器 WebRTC API                          │
│  - Socket.io 客户端                           │
└─────────────────────────────────────────────┘
                    ↓ WebSocket/Socket.io
┌─────────────────────────────────────────────┐
│      信令层 (Signaling Layer)                │
│  - Socket.io 连接管理                         │
│  - 房间和会议控制                              │
│  - 事件分发和广播                              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│      核心业务层 (Core Business Layer)         │
│  - Peer 生命周期管理                          │
│  - Transport 创建和连接                       │
│  - Producer/Consumer 管理                    │
│  - Room 状态维护                              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Mediasoup 媒体层 (Media Layer)            │
│  - Worker 进程池                             │
│  - Router 路由管理                            │
│  - RTP 能力协商                               │
│  - 媒体流转发                                 │
└─────────────────────────────────────────────┘
                    ↓ gRPC
┌─────────────────────────────────────────────┐
│    Spring Cloud 集成层                        │
│  - 用户认证授权 (gRPC)                        │
│  - 房间权限验证 (gRPC)                        │
│  - 服务注册发现 (Nacos)                       │
│  - 业务事件同步 (gRPC)                        │
└─────────────────────────────────────────────┘
```

### 技术栈

| 分类           | 技术                 | 版本  | 用途                 |
| -------------- | -------------------- | ----- | -------------------- |
| **运行时**     | Node.js              | 20+   | JavaScript 运行环境  |
| **语言**       | TypeScript           | 5.9+  | 类型安全的开发语言   |
| **媒体服务器** | Mediasoup            | 3.19+ | SFU 核心引擎         |
| **信令通信**   | Socket.io            | 4.8+  | WebSocket 实时通信   |
| **RPC 框架**   | gRPC (@grpc/grpc-js) | 1.14+ | 与 Spring Cloud 通信 |
| **服务发现**   | Nacos                | 2.x   | 服务注册与配置管理   |
| **Web 框架**   | Express              | 5.2+  | HTTP 服务和 API      |

---

## 📁 目录结构

```
sfu/
├── src/                          # 源代码目录
│   ├── server.ts                 # 服务器入口文件
│   ├── config/                   # 配置模块
│   │   └── config.ts             # 统一配置管理
│   ├── core/                     # 核心业务模块
│   │   ├── peer.ts               # Peer 实体类
│   │   └── room-manager.ts       # 房间管理器
│   ├── mediasoup/                # Mediasoup 封装
│   │   ├── mediasoup-manager.ts  # Worker/Router 管理
│   │   └── room.ts               # 房间实体类
│   ├── signaling/                # 信令处理模块
│   │   └── signaling-handler.ts  # Socket.io 事件处理
│   └── utils/                    # 工具模块
│       ├── grpc-client.ts        # gRPC 客户端封装
│       ├── nacos-client.ts       # Nacos 客户端封装
│       └── logger.ts             # 日志工具
├── proto/                        # gRPC Proto 定义
│   └── sfu-service.proto         # SFU 服务接口定义
├── dist/                         # 编译输出目录
├── logs/                         # 日志文件目录
├── .env                          # 环境变量配置
├── tsconfig.json                 # TypeScript 配置
├── package.json                  # 项目依赖配置
├── Dockerfile                    # Docker 镜像构建
├── docker-compose.yml            # Docker 编排配置
└── README.md                     # 项目文档
```

---

## 🚀 快速开始

### 环境要求

- **Node.js**: >= 20.0.0
- **npm**: >= 9.0.0
- **操作系统**: Linux/macOS/Windows
- **端口需求**:
  - 3000 (HTTP/WebSocket)
  - 40000-49999 (RTP/UDP/TCP)

### 安装依赖

```bash
# 克隆项目
cd sfu

# 安装依赖
npm install
```

### 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件
nano .env
```

**关键配置项说明：**

```env
# 服务器配置
PORT=3000                           # HTTP 服务端口
HOST=0.0.0.0                        # 监听地址
NODE_ENV=development                # 运行环境

# Mediasoup 配置
MEDIASOUP_WORKERS=4                 # Worker 进程数
MEDIASOUP_LISTEN_IP=0.0.0.0         # RTP 监听地址
MEDIASOUP_ANNOUNCED_IP=your_ip      # 公网 IP（重要！）
RTC_MIN_PORT=40000                  # RTP 端口范围起始
RTC_MAX_PORT=49999                  # RTP 端口范围结束

# gRPC 配置
GRPC_HOST=localhost                 # Spring Cloud 服务地址
GRPC_PORT=50051                     # gRPC 服务端口

# Nacos 配置
NACOS_SERVER=127.0.0.1:8848         # Nacos 服务器地址
NACOS_NAMESPACE=public              # 命名空间
```

### 开发模式运行

```bash
# 启动开发服务器（热重载）
npm run dev
```

### 生产模式运行

```bash
# 构建项目
npm run build

# 启动生产服务
npm run start
```

---

## 🔧 核心功能

### 1. 房间管理

- **创建房间**: 自动创建 Mediasoup Router
- **加入房间**: 用户身份验证和权限检查
- **离开房间**: 清理资源和通知其他参与者
- **自动销毁**: 最后一个参与者离开时自动关闭房间

### 2. 媒体流管理

#### 发布媒体流 (Produce)

```typescript
// 客户端流程
1. createWebRtcTransport({ producing: true })
2. connectWebRtcTransport({ dtlsParameters })
3. produce({ kind, rtpParameters })
```

#### 订阅媒体流 (Consume)

```typescript
// 客户端流程
1. createWebRtcTransport({ consuming: true })
2. connectWebRtcTransport({ dtlsParameters })
3. consume({ producerId, rtpCapabilities })
4. resumeConsumer({ consumerId })
```

### 3. gRPC 集成功能

| 功能       | gRPC 方法                 | 说明                       |
| ---------- | ------------------------- | -------------------------- |
| 房间验证   | `ValidateRoomAccess`      | 验证用户是否有权限进入房间 |
| Token 验证 | `ValidateUserToken`       | 验证用户身份令牌           |
| 加入通知   | `NotifyParticipantJoined` | 通知业务层用户加入事件     |
| 离开通知   | `NotifyParticipantLeft`   | 通知业务层用户离开事件     |
| 统计上报   | `ReportMediaStats`        | 上报媒体质量统计数据       |

---

## 📡 API 接口

### HTTP 接口

#### 健康检查

```http
GET /health

Response:
{
  "status": "healthy",
  "timestamp": "2024-12-03T10:30:00.000Z",
  "uptime": 3600.5
}
```

#### 房间统计

```http
GET /api/stats

Response:
{
  "totalRooms": 3,
  "rooms": [
    {
      "id": "room-001",
      "participants": 5,
      "peers": [
        {
          "id": "peer-abc",
          "userId": "user-123",
          "username": "Alice",
          "producers": 2,
          "consumers": 8
        }
      ]
    }
  ]
}
```

### Socket.io 事件

#### 客户端 → 服务器

| 事件                       | 参数                                      | 说明                |
| -------------------------- | ----------------------------------------- | ------------------- |
| `joinRoom`                 | `{ roomId, userId, username, token }`     | 加入房间            |
| `getRouterRtpCapabilities` | `{ roomId }`                              | 获取路由器 RTP 能力 |
| `createWebRtcTransport`    | `{ roomId, producing, consuming }`        | 创建 WebRTC 传输层  |
| `connectWebRtcTransport`   | `{ roomId, transportId, dtlsParameters }` | 连接传输层          |
| `produce`                  | `{ roomId, kind, rtpParameters }`         | 发布媒体流          |
| `consume`                  | `{ roomId, producerId, rtpCapabilities }` | 订阅媒体流          |
| `resumeConsumer`           | `{ roomId, consumerId }`                  | 恢复消费者          |
| `pauseProducer`            | `{ roomId, producerId }`                  | 暂停生产者          |
| `resumeProducer`           | `{ roomId, producerId }`                  | 恢复生产者          |
| `closeProducer`            | `{ roomId, producerId }`                  | 关闭生产者          |
| `leaveRoom`                | -                                         | 离开房间            |

#### 服务器 → 客户端

| 事件              | 数据                           | 说明         |
| ----------------- | ------------------------------ | ------------ |
| `newPeer`         | `{ peerId, userId, username }` | 新参与者加入 |
| `peerLeft`        | `{ peerId, userId, username }` | 参与者离开   |
| `newProducer`     | `{ producerId, peerId, kind }` | 新媒体流可用 |
| `producerPaused`  | `{ producerId, peerId }`       | 生产者已暂停 |
| `producerResumed` | `{ producerId, peerId }`       | 生产者已恢复 |
| `producerClosed`  | `{ producerId, peerId }`       | 生产者已关闭 |

---

## 🐳 Docker 部署

### 构建镜像

```bash
# 构建 Docker 镜像
npm run docker:build

# 或手动构建
docker build -t sfu-server:latest .
```

### 运行容器

```bash
# 单独运行
docker run -d \
  --name sfu-server \
  -p 3000:3000 \
  -p 40000-40100:40000-40100/udp \
  --env-file .env \
  sfu-server:latest

# 使用 docker-compose
docker-compose up -d
```

### Docker Compose 配置

```yaml
version: "3.8"

services:
  sfu-server:
    build: .
    ports:
      - "3000:3000"
      - "40000-40100:40000-40100/udp"
    environment:
      - MEDIASOUP_ANNOUNCED_IP=${PUBLIC_IP}
      - GRPC_HOST=spring-cloud-service
      - NACOS_SERVER=nacos:8848
    depends_on:
      - nacos
    restart: unless-stopped
```

---

## 🔍 监控与调试

### 日志级别

在 `.env` 中配置：

```env
MEDIASOUP_LOG_LEVEL=warn  # debug | warn | error
```

### 性能指标

访问统计接口获取实时数据：

```bash
curl http://localhost:3000/api/stats
```

---

## 🔐 安全建议

### 生产环境配置

1. **启用 HTTPS/WSS**

```javascript
// 使用 SSL 证书
const options = {
	key: fs.readFileSync("key.pem"),
	cert: fs.readFileSync("cert.pem"),
};
const server = https.createServer(options, app);
```

2. **限制 CORS**

```env
CORS_ORIGIN=https://your-domain.com
```

3. **启用 Token 验证**

- 所有 `joinRoom` 请求必须携带有效 JWT Token
- gRPC 验证确保业务层权限检查

4. **网络隔离**

- 将 gRPC 服务置于内网
- 使用防火墙限制 RTP 端口访问

---

## 📊 性能优化

### Worker 数量配置

```env
# CPU 核心数的 50-100%
MEDIASOUP_WORKERS=4
```

### RTP 端口范围

```env
# 根据并发用户数调整
# 每个用户约需要 4-6 个端口
RTC_MIN_PORT=40000
RTC_MAX_PORT=49999
```

### 编解码器优先级

在 `config.ts` 中调整 `mediaCodecs` 顺序，优先使用性能更好的编解码器。

---

## 🤝 与 Spring Cloud 集成

### 职责划分

| 功能模块   | SFU Server | Spring Cloud |
| ---------- | ---------- | ------------ |
| 媒体流转发 | ✅         | ❌           |
| 信令处理   | ✅         | ❌           |
| 用户认证   | ❌         | ✅           |
| 房间权限   | ❌         | ✅           |
| 业务逻辑   | ❌         | ✅           |
| 数据持久化 | ❌         | ✅           |
| 会议录制   | ❌         | ✅           |

### 通信流程

```
客户端加入房间:
1. 客户端 → SFU: joinRoom(roomId, userId, token)
2. SFU → Spring Cloud (gRPC): ValidateRoomAccess(roomId, userId, token)
3. Spring Cloud → SFU: { allowed: true, config: {...} }
4. SFU → 客户端: { peerId, peers: [...] }
5. SFU → Spring Cloud (gRPC): NotifyParticipantJoined(...)
```

---

## 📝 开发指南

### 添加新的信令事件

1. 在 `signaling-handler.ts` 中添加事件监听
2. 实现业务逻辑
3. 必要时调用 gRPC 同步到 Spring Cloud

### 扩展 gRPC 服务

1. 修改 `proto/sfu-service.proto`
2. 在 `grpc-client.ts` 中添加方法
3. 重启服务加载新定义

---
