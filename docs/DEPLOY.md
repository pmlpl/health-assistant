# 部署指南

本文档为**唯一**生产部署参考。原 `DEPLOY_INSTRUCTIONS.md`、`DEPLOY_STEPS.md`（含根目录副本）已合并至此并删除。

## 环境要求

- JDK 21+
- MySQL 8.0+
- Node.js 18+（构建前端）
- Maven 3.8+ 或 `backend/mvnw`

## 1. 环境变量

在服务器环境或 `backend/.env` 中配置：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=health_assistant
DB_USERNAME=root
DB_PASSWORD=你的密码

JWT_SECRET=至少32位随机字符串
CORS_ALLOWED_ORIGINS=https://你的前端域名
APP_SEED_TEST_USER=false

# 生产建议：云端 Key 由用户在应用内「AI 设置」自备（BYOK）
DEEPSEEK_API_KEY=
DOUBAO_API_KEY=
DASHSCOPE_API_KEY=

AI_SETTINGS_SECRET=至少32位随机字符串
AI_DEPLOYMENT_MODE=prod
APP_UPLOAD_DIR=/var/app/uploads
```

完整说明见 [backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md)。

## 2. 构建

```powershell
# 后端
cd backend
.\mvnw.cmd clean package -DskipTests

# 前端（构建前设置 VITE_API_BASE_URL）
cd frontend
npm ci
npm run build
```

产物：

- 后端 JAR：`backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar`
- 前端静态文件：`frontend/dist/`

## 3. 启动后端

```bash
java -jar HealthAssistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Railway：使用 [`backend/railway.json`](../backend/railway.json) 中的启动命令。

后台运行示例：

```bash
nohup java -jar HealthAssistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
```

## 4. 部署前端

### Netlify / Vercel

- 构建命令：`npm run build`
- 发布目录：`frontend/dist`
- 环境变量：`VITE_API_BASE_URL=https://你的API域名/api`
- SPA 回退：仓库已含 [`netlify.toml`](../netlify.toml)（`/* → /index.html`）

### Nginx 示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # SSE（AI 流式）需禁用缓冲
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 600s;
    }
}
```

### 前后端分离（无 Nginx 代理）

前端 `.env.production` 指向完整后端地址（含 `/api`），浏览器直接跨域请求后端，需正确配置 `CORS_ALLOWED_ORIGINS`。

## 5. AI 与媒体能力

| 能力 | 说明 |
|------|------|
| 文本 AI | 生产：用户 **AI 设置** BYOK（DeepSeek / 通义 / LM Studio）；开发：`.env` 平台 Key + dev profile |
| 拍照识食 | 豆包视觉 / LM Studio 视觉 / 通义，按用户设置与 `AI_IMAGE_RECOGNITION_ORDER` |
| 食谱配图 | Pexels 搜图 + 豆包 Seedream 生图；同名食谱复用已存配图 |
| 平台试用 | `AI_PLATFORM_TRIAL_*` 控制未自备 Key 用户的试用次数 |

本地开发：LM Studio 默认 `http://127.0.0.1:1234/v1`，`spring.profiles.active=dev`。

## 6. 验证清单

- [ ] 前端可访问，登录正常
- [ ] `GET /api/status/api-availability` 返回 AI 状态
- [ ] `GET /actuator/health` 正常（若已暴露）
- [ ] Flyway 迁移成功（prod 空库自动建表）
- [ ] AI 设置 → 连接测试 → AI 精灵对话有响应
- [ ] 上传目录 `APP_UPLOAD_DIR` 可写（食谱配图）

## 7. 数据库运维

### Flyway

`prod` profile 启动时自动执行 `db/migration/` 脚本。全新库只需创建空库 `health_assistant`。

### 备份（示例）

```powershell
mysqldump -h localhost -u root -p health_assistant > "backup_$(Get-Date -Format yyyyMMdd).sql"
```

## 8. 常见问题

| 问题 | 处理 |
|------|------|
| 401 未登录 | 重新登录；检查 JWT 与 CORS |
| CORS 错误 | `CORS_ALLOWED_ORIGINS` 包含前端域名 |
| Railway 启动失败 | JAR 名 `HealthAssistant-0.0.1-SNAPSHOT.jar` |
| AI 无响应 | 用户是否保存 AI 设置；生产是否 BYOK |
| SSE 卡住 | Nginx 关闭 `proxy_buffering`；或本地 LM 走同步接口 |

## 相关文件

- [`netlify.toml`](../netlify.toml) — Netlify SPA 回退
- [`backend/railway.json`](../backend/railway.json) — Railway 启动配置
- [`frontend/API_CONFIGURATION_GUIDE.md`](../frontend/API_CONFIGURATION_GUIDE.md) — 前端 API 地址
