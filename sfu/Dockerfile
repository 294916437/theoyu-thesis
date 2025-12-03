FROM node:20-alpine AS builder

WORKDIR /app

# 安装构建依赖
RUN apk add --no-cache python3 make g++

# 复制包管理文件
COPY package*.json ./
COPY tsconfig.json ./

# 安装依赖
RUN npm ci --only=production && \
    npm cache clean --force

# 复制源代码
COPY src ./src
COPY proto ./proto

# 构建
RUN npm run build

# 生产镜像
FROM node:20-alpine

WORKDIR /app

# 安装运行时依赖
RUN apk add --no-cache python3 make g++

# 复制构建产物
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 3000
EXPOSE 40000-49999/udp
EXPOSE 40000-49999/tcp

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# 启动应用
CMD ["node", "dist/server.js"]