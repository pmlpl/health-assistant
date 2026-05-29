# 快速启动指南

## 环境要求

| 环境 | 版本 |
|------|------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.8+（或使用项目 `mvnw`） |

## 启动步骤

### 1. 数据库

创建空库 `health_assistant`（开发 profile 下 JPA 可自动建表；生产请用 Flyway，见 [DEPLOY.md](./DEPLOY.md)）。

### 2. 后端配置

```powershell
cd backend
Copy-Item .env.example .env
# 编辑 .env：DB_PASSWORD、JWT_SECRET、按需填写 AI Key
.\mvnw.cmd spring-boot:run
```

默认 `spring.profiles.active=dev`，本地 LM Studio 可在应用内 **AI 设置** 配置，无需在 `.env` 写 Key。

详细变量说明：[backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md)

### 3. 前端

```powershell
cd frontend
npm install
npm run dev
```

确保存在 `frontend/.env.development`：

```env
VITE_API_BASE_URL=/api
```

### 4. 访问

- 前端：http://localhost:5173
- 后端：http://localhost:8080/test/health

## 启动前检查

- [ ] MySQL 已运行
- [ ] `backend/.env` 已配置数据库与 JWT
- [ ] `frontend/.env.development` 为 `VITE_API_BASE_URL=/api`
- [ ] 使用 AI 功能时已在 Header → **AI 设置** 配置服务商，或 `.env` 中配置了平台试用 Key

## 本地 LM Studio（可选）

1. 启动 LM Studio，加载模型并开启 Local Server（默认 `http://127.0.0.1:1234/v1`）
2. 登录应用 → Header → **AI 设置** → 文本服务商选「本地 LM Studio」
3. 填写地址与模型名 → **测试连接**（成功后会自动保存）
4. 打开 **AI 精灵** 发送消息；本地 LM 走同步接口，LM 终端应出现 `/v1/chat/completions` 请求

## 常见问题

### 端口被占用

```powershell
netstat -ano | findstr :5173
taskkill /PID <进程ID> /F
```

或在 `frontend/vite.config.js` 修改 `server.port`。

### API 401 / 连接失败

1. 确认后端已启动
2. 重新登录获取 JWT（`localStorage.authToken`）
3. 检查 Network 请求是否带 `Authorization: Bearer ...`

### AI 一直 loading、LM 无请求

1. AI 设置中 **测试连接** 后确认已 **保存**
2. 文本服务商应为「本地 LM Studio」而非仅测试表单未保存
3. 重启前后端后再试

### 生产构建

```powershell
# frontend/.env.production
VITE_API_BASE_URL=https://你的后端域名/api

cd frontend
npm run build
```

修改 `.env.production` 后必须重新 `npm run build`。

## 更多文档

- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) — 目录与路由
- [DEPLOY.md](./DEPLOY.md) — 生产部署
- [frontend/API_CONFIGURATION_GUIDE.md](../frontend/API_CONFIGURATION_GUIDE.md) — 前端 API 配置
