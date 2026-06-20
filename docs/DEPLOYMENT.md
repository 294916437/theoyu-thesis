# 项目开发与部署说明

## 1. 环境要求

- Node.js 20+、npm 9+
- JDK 17+、Maven 3.8+
- Docker、Docker Compose
- Windows PowerShell 或兼容终端

## 2. 启动中间件

中间件配置位于 `deploy/docker`。

```powershell
cd deploy/docker/base
docker compose up -d

cd ../rocketmq
docker compose up -d
```

`base/docker-compose.yml` 包含 MySQL、Redis、MinIO、Nacos、Cassandra、ZooKeeper；`rocketmq/docker-compose.yml` 包含 RocketMQ NameServer、Broker、Dashboard。

注意：compose 文件中存在 `/mnt/e/linux/docker/...` 这类本机挂载路径，部署前需要按实际服务器路径调整，并提前准备 MySQL、Redis、Nacos、RocketMQ 等配置文件。

## 3. 启动后端 SpringCloud

进入后端目录：

```powershell
cd backend
```

推荐使用根目录脚本启动：

```powershell
.\start-all.ps1
```

脚本会先提示是否编译模块，可输入 `all` 全量编译，或输入序号选择模块编译，也可直接回车跳过。随后脚本会提示选择要启动的服务，输入 `all` 启动全部服务。

主要服务包括：

- `thesis-gateway`
- `thesis-auth`
- `thesis-user/thesis-user-biz`
- `thesis-oss/thesis-oss-biz`
- `thesis-kv/thesis-kv-biz`
- `thesis-id-generator/thesis-id-generator-biz`
- `thesis-media/thesis-media-biz`
- `thesis-chat/thesis-chat-biz`

脚本会从各模块 `target` 目录查找 jar，并用 `java -jar` 分窗口启动。网关默认由前端代理到 `8000` 端口，实际端口以各服务配置为准。

## 4. 启动 Node.js SFU

进入 SFU 目录并安装依赖：

```powershell
cd sfu
npm install
```

复制并修改环境变量：

```powershell
copy .env.example .env
```

重点确认：

- `PORT`：SFU HTTP/Socket.io 端口，默认 `3000`
- `MEDIASOUP_ANNOUNCED_IP`：生产环境填写公网 IP 或可访问 IP
- `RTC_MIN_PORT`、`RTC_MAX_PORT`：WebRTC RTP 端口范围
- `GRPC_HOST`、`GRPC_PORT`：SpringCloud gRPC 服务地址
- `NACOS_SERVER`、`NACOS_IP`、`NACOS_PORT`：Nacos 注册配置

开发模式：

```powershell
npm run dev
```

生产模式：

```powershell
npm run build
npm run start
```

本地多实例启动时，需要区分 `PORT`、`NACOS_PORT` 和 RTP 端口段，例如：

```powershell
$env:PORT="3001"; $env:NACOS_PORT="3001"; $env:SFU_INSTANCE_ID="sfu-local-3001"; $env:RTC_MIN_PORT="41000"; $env:RTC_MAX_PORT="41999"; npm run start
```

## 5. 启动前端

进入前端目录并安装依赖：

```powershell
cd frontend
npm install
```

按环境修改 `.env`。开发环境通常保持相对路径，由 Vite 代理到后端：

- `/api` 代理到 SpringCloud Gateway
- `/message/ws`、`/media/ws` 代理到后端 WebSocket
- `/sfu/{port}/socket.io` 代理到本地 SFU

开发启动：

```powershell
npm run dev
```

生产构建：

```powershell
npm run build
```

构建产物位于 `frontend/dist`，可部署到 Nginx 等静态服务器。生产环境需要将前端环境变量中的 WebSocket、SFU 地址改为实际域名或反向代理地址，例如 `wss://your-domain.com/...`。

## 6. 推荐启动顺序

1. 启动 Docker 中间件。
2. 确认 Nacos、MySQL、Redis、RocketMQ 等服务可用。
3. 启动 SpringCloud 后端。
4. 启动 SFU。
5. 启动或部署前端。

## 7. 常用检查

```powershell
# 查看中间件容器
docker ps

# 检查 SFU 健康状态
curl http://localhost:3000/health

# 检查前端生产构建
cd frontend
npm run build
```
