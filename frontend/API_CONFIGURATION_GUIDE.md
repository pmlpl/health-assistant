# 前端 API 配置指南

## 概述

前端通过环境变量 `VITE_API_BASE_URL` 决定 API 根地址，由 `src/api/healthApi.js` 统一封装 Axios 与 JWT。

- **开发**：`VITE_API_BASE_URL=/api`，Vite 代理到 `http://localhost:8080`
- **生产**：完整后端地址，建议含 `/api` 后缀，如 `https://api.example.com/api`

## 环境文件

### `.env.development`

```env
VITE_API_BASE_URL=/api
```

### `.env.production`（勿提交 Git）

```env
VITE_API_BASE_URL=https://你的后端域名/api
```

### 可选：AI 流式超时

```env
VITE_AI_STREAM_FIRST_BYTE_MS=90000
VITE_AI_STREAM_IDLE_MS=120000
VITE_AI_STREAM_TOTAL_MS=600000
```

修改 `.env.production` 后必须重新 `npm run build`。

## 本地开发

```powershell
# 终端 1
cd backend
.\mvnw.cmd spring-boot:run

# 终端 2
cd frontend
npm run dev
```

访问 http://localhost:5173 。请求 `/api/*` 由 Vite 转发到后端。

### Vite 代理（`vite.config.js`）

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    // SSE：AI 流式需禁用代理缓冲
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => {
        proxyReq.setHeader('Accept', 'text/event-stream');
      });
    },
  },
},
```

## 生产部署

| 平台 | 配置 |
|------|------|
| Netlify | 构建 env：`VITE_API_BASE_URL`；SPA 回退见根目录 `netlify.toml` |
| Nginx 同域 | 前端静态 + `/api` 反代后端 |
| 前后端分离 | `VITE_API_BASE_URL` 指向后端完整 URL，后端配置 CORS |

构建：

```powershell
cd frontend
npm run build
# 产物在 dist/
```

## 鉴权

- Token 存储键：`localStorage.authToken`（非 `token`）
- `healthApi.js` 请求拦截器自动附加 `Authorization: Bearer ...`
- 401 时需重新登录

所有页面应通过 `healthApi` / `apiClient` 发请求，避免裸 `fetch` 导致 401（如日历页已修复）。

## AI 请求路径

| 场景 | 接口 |
|------|------|
| 营养咨询（同步） | `POST /api/ai/nutrition-advice` |
| 营养咨询（流式） | `POST /api/ai/nutrition-advice/stream` |
| 连接测试 | `POST /api/users/me/ai-settings/test` |

本地 LM Studio 时前端优先走**同步**接口；云端模型可走 SSE 流式，超时后自动降级同步。

逻辑见 `src/services/aiStreamRunner.js`、`src/composables/useAIConsult.js`。

## 常见问题

### Q: 开发环境 API 失败？

1. 后端是否在 8080 运行
2. `.env.development` 是否为 `/api`
3. 浏览器 Network 查看实际 URL 与状态码

### Q: 生产环境仍请求 localhost？

未在构建时注入 `VITE_API_BASE_URL`，或部署了旧 `dist`。重新 build 并确认 CI 环境变量。

### Q: CORS 错误？

后端 `CORS_ALLOWED_ORIGINS` 需包含前端域名。

### Q: `/api` 要不要写在 VITE_API_BASE_URL 里？

`healthApi.js` 会规范化：若生产地址未含 `/api` 会自动补全。推荐直接写 `https://host/api` 避免歧义。

## 相关文档

- [docs/QUICK_START.md](../docs/QUICK_START.md)
- [docs/DEPLOY.md](../docs/DEPLOY.md)
- [backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md)
