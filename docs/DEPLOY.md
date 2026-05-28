# 部署指南

本文档合并了原有多份部署说明，作为唯一生产部署参考。

## 环境要求

- JDK 21+
- MySQL 8.0+
- Node.js 18+（仅构建前端时需要）
- Maven 3.8+ 或项目自带 `mvnw`

## 1. 配置环境变量

在服务器或 `backend/.env` 中设置：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=health_assistant
DB_USERNAME=root
DB_PASSWORD=你的密码

JWT_SECRET=至少32位随机字符串
CORS_ALLOWED_ORIGINS=https://你的前端域名
APP_SEED_TEST_USER=false

# prod 建议留空，由用户在应用内「AI 设置」自备（BYOK）
DOUBAO_API_KEY=
DASHSCOPE_API_KEY=

AI_SETTINGS_SECRET=至少32位随机字符串用于加密用户Key
AI_DEPLOYMENT_MODE=prod
APP_UPLOAD_DIR=/var/app/uploads
```

## 2. 构建

```powershell
# 后端
cd backend
.\mvnw.cmd clean package -DskipTests

# 前端
cd frontend
npm ci
npm run build
```

产物：

- 后端 JAR：`backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar`
- 前端静态文件：`frontend/dist/`

## 3. 启动后端（生产 profile）

```bash
java -jar HealthAssistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Railway 部署时使用 [`backend/railway.json`](../backend/railway.json) 中的启动命令。

## 4. 部署前端

将 `frontend/dist/` 内容部署到 Nginx、Vercel 或 Netlify：

- Nginx：配置静态目录与 `/api` 反向代理到后端
- Vercel/Netlify：已包含 SPA 回退配置（`vercel.json` / `netlify.toml`）

前端生产环境需在构建时设置：

```env
VITE_API_BASE_URL=https://你的API域名/api
```

**注意**：`.env.production` 不应提交到 Git，请在 CI 或平台环境变量中配置。

## 5. AI 与食谱配图

| 能力 | 说明 |
|------|------|
| 文本 AI | 开发：`dev` profile + 本机 LM Studio；生产：用户登录后在 Header → **AI 设置** 填写 DashScope Key 或 LM 穿透地址 |
| 食谱配图 | Pexels 搜真实图 + 下载到 `uploads/recipes/`，需用户在 AI 设置中配置 **Pexels Key** |
| 费用 | 生产环境服务端**不配置**云端 Key，由用户各自承担 |

本地开发：启动 LM Studio（Qwen3.5:9b），Local Server 默认 `http://127.0.0.1:1234/v1`，`spring.profiles.active=dev`。

## 6. 验证

- 访问前端首页，使用账号登录
- 检查 `GET /api/status/api-availability` 返回 AI 配置状态
- 查看后端日志确认 Flyway 迁移成功（prod profile）
- 检查 Actuator：`GET /actuator/health`、`GET /actuator/metrics/hikaricp.connections`

## 7. 数据库运维

### Flyway 迁移

生产环境（`prod` profile）启动时自动执行 `db/migration/V1`–`V5`：

- `V2`：完整建表（新库）
- `V3`：性能索引 + 图片字段 LONGTEXT
- `V4`：ElementCollection 唯一约束
- `V5`：预留 `user_profile_id` 列（未来 FK 迁移）

全新 MySQL 库只需创建空库 `health_assistant`，以 `prod` profile 启动即可自动建表。

### 慢查询日志

在 MySQL 配置（`my.cnf` 或云控制台）中启用：

```ini
slow_query_log=1
slow_query_log_file=/var/log/mysql/slow.log
long_query_time=1
```

开发环境可在 `application-dev.properties` 临时开启 `spring.jpa.show-sql=true` 对比 SQL 条数。

### 定时备份

```powershell
# 每日备份示例（Windows 任务计划程序）
mysqldump -h localhost -u root -p health_assistant > "C:\backup\health_assistant_$(Get-Date -Format yyyyMMdd).sql"
```

建议保留 7–30 天备份；恢复前先在测试库验证。

### 数据归档（可选）

当 `diet_record` / `fitness_record` 超过 2 年且业务允许时，可归档至 `_archive` 表：

```sql
INSERT INTO diet_record_archive SELECT * FROM diet_record WHERE date < DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
DELETE FROM diet_record WHERE date < DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
```

大量 DELETE 后可对表执行 `OPTIMIZE TABLE`（低峰期进行）。

## 8. 常见问题

| 问题 | 处理 |
|------|------|
| 401 未登录 | 重新登录获取 JWT |
| CORS 错误 | 检查 `CORS_ALLOWED_ORIGINS` |
| Railway 启动失败 | 确认 JAR 名为 `HealthAssistant-0.0.1-SNAPSHOT.jar` |
| prod 启动 schema 错误 | 确保使用 `prod` profile；Flyway V2+ 会在空库自动建表 |

## 相关文件

- 根目录 [`DEPLOY_STEPS.md`](../DEPLOY_STEPS.md)（历史快速步骤，逐步迁移至本文档）
- [`docs/DEPLOY_INSTRUCTIONS.md`](DEPLOY_INSTRUCTIONS.md)（详细手动部署）
