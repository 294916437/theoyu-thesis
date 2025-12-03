# 基于 WebRTC 的实时视频会议系统设计与优化

## 项目简介

本项目是本科毕业论文的工程实现,旨在设计并实现一个基于 WebAssembly 和 WebRTC 技术的实时视频会议系统,重点研究性能优化策略。

## 技术栈

- **WebRTC**: 实时通信框架
- **WebAssembly**: 高性能计算模块
- **Rust**: 编译为 wasm 模块
- **前端**: Vue.js (用户交互页面)
- **后端**: SpringCloud (信令服务器和基础业务)

## 主要功能

- [ ] 多人实时音视频通话
- [ ] WebAssembly 加速的端侧视频处理
- [ ] 屏幕共享
- [ ] 录制回放
- [ ] 性能监控与优化

## 项目结构

```
theoyu-thesis/
├── frontend/          # 前端代码
├── backend/           # 后端信令服务器
├── wasm/              # WebAssembly 模块
├── sfu/               # sfu服务器
├── docs/              # 项目文档
└── README.md
```

## 快速开始

### 环境要求

## 研究重点

1. WebAssembly 在视频编解码中的性能优化
2. WebRTC 连接质量优化策略
3. 多人会议的资源调度算法
4. 网络自适应传输机制

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
cargo run
```

## 开发进度

- [ ] 需求分析与系统设计
- [ ] WebRTC 基础功能实现
- [ ] WebAssembly 模块开发
- [ ] 性能测试与优化
- [ ] 论文撰写

## 许可证

MIT License

## 作者

本科毕业论文项目

版权所有 Copyright © 2025-2026 by **Theoyu Du**
