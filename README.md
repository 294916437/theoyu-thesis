# 基于 WebRTC 的实时视频会议系统设计与优化

## 项目简介

本项目是本科毕业论文的工程实现,旨在设计并实现一个基于 WebAssembly 和 WebRTC 技术的实时视频会议系统,重点研究系统架构与性能优化

## 技术栈

- **WebRTC**: 实时通信框架
- **WebAssembly**: 高性能计算模块
- **Rust**: 编译为 wasm 模块
- **前端**: Vue.js (用户交互页面)
- **后端业务**: SpringCloud (Stomp 信令服务和业务逻辑)
- **后端 SFU**: Node.js (Socket.io 信令服务和 SFU 服务)

## 主要功能

- [x] 多人实时音视频通话(基于 Mediasoup 构建的 SFU 服务)
- [ ] 端侧视频处理(背景模糊...)
- [x] 屏幕共享
- [ ] 录制回放
- [ ] 性能监控与优化
- [x] 用户私聊(包含 P2P 视频通话)
- [ ] 会议房间聊天
- [ ] 会议数据统计
- [ ] 好友列表(提供会议候选者)

## 项目结构

```
theoyu-thesis/
├── frontend/          # Vue.js
├── backend/           # SpringCloud
├── wasm/              # WebAssembly 模块
├── sfu/               # Node.js
├── docs/              # 项目文档
└── README.md
```

## 快速开始

### 环境要求

## 研究重点

1. WebAssembly 在视频编解码中的性能优化
2. 多人会议的资源调度算法
3. 网络自适应传输机制
4. 异构微服务架构
5. 信令服务(Socket.io&Stomp)

# 项目结构

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

### SFU 开发

```bash
cd sfu
npm run dev
```

## 开发进度

- [ ] 需求分析与系统设计
- [ ] WebRTC 核心功能实现
- [ ] SFU 服务器实现
- [ ] SpringCloud 基础业务
- [ ] WebAssembly 模块开发
- [ ] 性能测试与优化
- [ ] 论文撰写

## 许可证

MIT License

## 作者

本科毕业论文项目

版权所有 Copyright © 2025-2026 by **Theoyu Du**
