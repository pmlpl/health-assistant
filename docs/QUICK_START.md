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
# 编辑 .env：DB_PASSWORD、JWT_SECRET、DEEPSEEK_API_KEY 等（见 .env.example 注释）
.\mvnw.cmd spring-boot:run
```

**AI 默认行为**：开发环境下，配置 `DEEPSEEK_API_KEY` 后，文本对话（AI 精灵、营养分析等）默认使用 **DeepSeek**，无需本地 LM Studio。本地 LM Studio 仅在 **AI 设置** 中显式选择「本地 LM Studio」时使用。

详细变量说明：[backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md)  
变量模板：[backend/.env.example](../backend/.env.example)（勿将填好密钥的 `.env` 提交 Git）

### 3. 前端

```powershell
cd frontend
npm install
npm run dev
```

确保存在 `frontend/.env.development`（可参考 [frontend/.env.example](../frontend/.env.example)）：

```env
VITE_API_BASE_URL=/api
```

### 4. 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173（端口占用时 Vite 自动改用 **5174**） |
| 后端 | http://localhost:8080 |
| 健康检查 | http://localhost:8080/test/health |

若使用 5174，请确认 `backend/.env` 中 `CORS_ALLOWED_ORIGINS` 包含 `http://localhost:5174`。

## 启动前检查

- [ ] MySQL 已运行
- [ ] `backend/.env` 已配置数据库与 JWT
- [ ] 需要 AI 功能时已在 `.env` 配置 `DEEPSEEK_API_KEY`（或登录后在 **AI 设置** BYOK）
- [ ] `frontend/.env.development` 为 `VITE_API_BASE_URL=/api`
- [ ] 后端已监听 8080，前端 dev server 已启动

## 健身记录（近期更新）

健身页（`/fitness`）支持：

- **记录维度多选**：时长、次数、重量、距离（如跑步可勾选「时长 + 距离」）
- **距离字段**：有氧类运动可填写公里数（`distance_km`），汇总区显示总里程
- 至少保留一种记录维度；热量可按所选维度自动估算

## 本地 LM Studio（可选 BYOK）

> 非默认路径。平台默认文本 AI 为 DeepSeek；仅当需要完全离线推理时再配置 LM Studio。

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

Vite 在 5173 被占用时会自动尝试 5174。也可在 `frontend/vite.config.js` 的 `server.port` 中指定固定端口。

### API 401 / 连接失败

1. 确认后端已启动（8080）
2. 重新登录获取 JWT（`localStorage.authToken`）
3. 检查 Network 请求是否带 `Authorization: Bearer ...`

### AI 一直 loading、无响应

1. 确认 `DEEPSEEK_API_KEY` 已配置，或 **AI 设置** 中已保存有效 BYOK
2. AI 设置中 **测试连接** 后确认已 **保存**
3. 若使用 LM Studio，确认 Local Server 已启动且模型已加载
4. 重启前后端后再试

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
