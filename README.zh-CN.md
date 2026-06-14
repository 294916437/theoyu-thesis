# 基于 WebRTC 的实时视频会议系统设计与实现

> 本科毕业论文工程实现 · 开源地址：[github.com/294916437/theoyu-thesis](https://github.com/294916437/theoyu-thesis)

[English](README.md)

## 项目简介

本项目是本科毕业论文的完整工程实现，聚焦于基于 WebRTC 与 WebAssembly 技术的实时视频会议系统的设计实现与性能优化，涵盖 SFU 多人音视频、端侧视频处理、异构微服务通信等核心研究方向。

## 技术栈

| 层级 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | Vue3 + Vuetify + Pinia + Vite | 用户交互界面 |
| 前端通信 | mediasoup-client + Socket.io-client + StompJS | WebRTC 客户端、信令 |
| 前端渲染 | Pixi.js | 欢迎页动画 |
| 业务后端 | Spring Cloud Alibaba + MySQL + Redis | 微服务业务逻辑 |
| 消息队列 | RocketMQ | 异步消息处理 |
| 服务注册 | Nacos | 服务注册与配置中心 |
| 存储 | MinIO + Cassandra | 对象存储与会议文本数据 |
| SFU 服务 | Node.js + Mediasoup + Socket.io | 流媒体转发服务 |
| 跨语言通信 | gRPC | Spring Cloud 与 SFU 通信 |
| 端侧处理 | Rust → WebAssembly | 浏览器端视频背景分割 |

## 主要功能

- [x] 多人实时音视频会议（基于 Mediasoup 构建 SFU 服务，支持带宽自适应）
- [x] 端侧视频处理（背景模糊与背景替换，Rust 编译为 WASM 在浏览器运行）
- [x] 屏幕共享
- [x] 会议录制与回放（持久化存储至 MinIO）
- [x] P2P 音视频通话（私聊场景，基于 WebRTC 直连）
- [x] 会议房间聊天与文件分享
- [x] 平台私聊中心（文字、文件、P2P 音视频）
- [x] 主持人权限控制（静音、移出、全体静音/关摄像头）
- [x] 会议参与者列表与实时状态
- [x] 会议数据统计与监控

## 项目预览

### 首页

![项目首页预览](assets/preview/home.png)

### 会议详情

![会议详情预览](assets/preview/detail.png)

### 会议房间

![会议房间预览](assets/preview/room.png)

## 项目结构

```text
theoyu-thesis/
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── features/                 # 业务模块（meeting / chat / user）
│       ├── components/               # 公共组件
│       ├── stores/                   # Pinia 状态管理
│       ├── services/                 # WebRTC / Socket.io / Stomp 服务层
│       ├── composables/              # 组合式函数
│       ├── api/                      # HTTP 接口封装
│       └── utils/                    # 工具函数
│
├── backend/                          # Spring Cloud 微服务后端
│   ├── thesis-gateway/               # API 网关（统一鉴权、路由）
│   ├── thesis-auth/                  # 认证授权服务
│   ├── thesis-user/                  # 用户管理服务
│   ├── thesis-media/                 # 会议管理服务
│   ├── thesis-chat/                  # 聊天服务（私聊 + 房间聊天）
│   ├── thesis-kv/                    # KV 缓存服务（Redis 封装）
│   ├── thesis-oss/                   # 对象存储服务（MinIO 封装）
│   ├── thesis-id-generator/          # 分布式 ID 生成服务
│   └── thesis-framework/             # 公共基础框架（common / logger / jackson）
│
├── sfu/                              # Node.js SFU 服务（TypeScript）
│   ├── src/
│   │   ├── core/                     # Room / Peer / Mediasoup 核心管理
│   │   ├── features/                 # Socket 处理、gRPC 服务端、录制、监控、延迟采集
│   │   ├── config/                   # 服务配置
│   │   └── utils/                    # 工具函数
│   └── proto/                        # gRPC 协议定义（.proto 文件）
│
├── wasm/                             # Rust → WebAssembly 端侧视频处理
│   └── src/
│       ├── lib.rs                    # 背景模糊与背景替换实现
│
├── test/                             # 性能测试套件
│   ├── concurrent-stress/            # SFU 并发压力测试
│   ├── e2e-latency/                  # 端到端延迟测试
│   ├── edge-side-performance-comparison/  # 端侧处理性能对比
│   └── jmeter-test/                  # 微服务接口压测
│
├── deploy/                           # 部署配置
│   ├── docker/                       # Docker Compose 及各服务镜像配置
│   ├── script/                       # 部署脚本
│   └── sql/                          # 数据库初始化脚本
│
├── demos/                            # WASM 端侧效果独立演示
└── docs/                             # 项目文档
```

## 快速开始

### 环境要求

- Node.js >= 22
- JDK >= 17
- Rust + wasm-pack
- Docker（用于中间件部署）

### 后端开发

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

### SFU 服务

```bash
cd sfu
npm install
npm run dev
```

### WASM 模块构建

```bash
cd wasm
wasm-pack build --target web
```

## 研究重点

1. 基于 SFU 架构的多人实时音视频，支持带宽自适应与多路媒体流转发。
2. Rust 编译为 WebAssembly 的端侧视频背景分割与实时处理性能优化。
3. Spring Cloud Alibaba 与 Node.js 异构微服务的 gRPC 跨语言通信。
4. 基于 WebRTC 的 P2P 音视频通话与信令设计。
5. 会议权限控制与主持人管理机制。

## 开发进度

- [x] 需求分析与系统设计
- [x] WebRTC 核心功能实现
- [x] SFU 服务器实现
- [x] Spring Cloud 微服务业务
- [x] WebAssembly 模块开发
- [x] 性能测试与优化
- [x] 论文撰写

## 许可证

本项目采用 [GNU General Public License v3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) 开源许可证。

更多请查看[LICENSE](./LICENSE)文件

SPDX-License-Identifier: GPL-3.0-or-later

## 作者

本科毕业论文项目

版权所有 Copyright (C) 2025-2026 **[Theoyu Du](https://github.com/294916437)**
