# 后端环境变量配置指南

敏感配置通过 `backend/.env` 管理（不提交 Git）。模板见 [`.env.example`](./.env.example)。

## 快速开始

```powershell
cd backend
Copy-Item .env.example .env
# 编辑 .env 后启动
.\mvnw.cmd spring-boot:run
```

Spring Boot 通过 `EnvConfig` 自动加载 `.env`。

## 变量说明

### 数据库

| 变量 | 说明 |
|------|------|
| `DB_HOST` | MySQL 主机，默认 `localhost` |
| `DB_PORT` | 端口，默认 `3306` |
| `DB_NAME` | 库名，默认 `health_assistant` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 |

### 安全

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | JWT 签名密钥，至少 32 字符 |
| `JWT_EXPIRATION_MS` | Token 有效期（毫秒） |
| `CORS_ALLOWED_ORIGINS` | 允许的前端源，逗号分隔 |
| `AI_SETTINGS_SECRET` | 加密用户自备 API Key |
| `APP_SEED_TEST_USER` | 开发 `true` 创建 testuser；生产 `false` |

### AI — 平台默认 Key（开发 / 试用）

| 变量 | 说明 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek，默认文本对话与食谱文案 |
| `AI_DEEPSEEK_MODEL` | 模型 ID，如 `deepseek-v4-flash` |
| `DOUBAO_API_KEY` | 豆包（火山方舟） |
| `DOUBAO_VISION_MODEL` | 视觉接入点 `ep-xxxx` |
| `DOUBAO_IMAGE_MODEL` | Seedream 生图模型 ID |
| `DOUBAO_IMAGE_SIZE` | 生图尺寸，如 `2K` |
| `DASHSCOPE_API_KEY` | 通义千问（可选） |

### AI — 平台试用配额

| 变量 | 说明 |
|------|------|
| `AI_PLATFORM_TRIAL_ENABLED` | 是否启用试用 |
| `AI_PLATFORM_TRIAL_TEXT_QUOTA` | 文本试用次数，`0` = 不限 |
| `AI_PLATFORM_TRIAL_IMAGE_QUOTA` | 识图试用次数 |
| `AI_PLATFORM_TRIAL_RECIPE_IMAGE_QUOTA` | 食谱配图试用次数 |
| `AI_IMAGE_RECOGNITION_ORDER` | 识图 fallback 顺序，如 `doubao,lmstudio,dashscope` |

### 部署

| 变量 | 说明 |
|------|------|
| `AI_DEPLOYMENT_MODE` | `dev` / `prod` |
| `APP_UPLOAD_DIR` | 食谱配图等上传目录 |

## 用户 AI 设置（BYOK）

生产环境推荐**不在服务端配置云端 Key**，由用户登录后在 Header → **AI 设置** 填写：

- **文本**：DeepSeek / 通义 / 本地 LM Studio / 自动
- **视觉**：豆包 / LM Studio / 通义
- **食谱配图**：Pexels Key + 豆包生图

设置加密存入数据库，由 `UserAiSettingsService` 解析。  
**连接测试**使用表单中的地址；**实际对话**使用已保存的设置——测试成功后务必点保存（测试 LM 成功会自动保存）。

### 本地 LM Studio

1. Local Server 默认 `http://127.0.0.1:1234/v1`
2. 在 AI 设置中选择「本地 LM Studio」，填写 `baseUrl` 与 `model`
3. 开发 profile（`dev`）下对话走非流式 `complete`，与连接测试一致

## 生产 vs 开发

| 场景 | Profile | Key 来源 |
|------|---------|----------|
| 本地开发 | `dev`（默认） | `.env` 平台 Key + 用户 AI 设置 |
| 生产 | `prod` | 用户 BYOK；`.env` 云端 Key 建议留空 |

启动生产：

```bash
java -jar HealthAssistant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 获取 API Key

- **DeepSeek**：https://platform.deepseek.com/
- **豆包 / 火山方舟**：https://www.volcengine.com/product/ark
- **通义千问**：https://dashscope.console.aliyun.com/
- **Pexels**（食谱搜图）：https://www.pexels.com/api/

## 故障排查

| 现象 | 检查 |
|------|------|
| API 调用失败「未配置」 | `.env` 是否存在、Key 是否正确、是否重启 |
| 数据库连接失败 | MySQL 运行中、库已创建、密码正确 |
| 测试通但对话无响应 | AI 设置是否已**保存**；`textProvider` 是否为 lmstudio |
| 识图失败 | 视觉 provider 与 `DOUBAO_VISION_MODEL` 是否为视觉 ep |
| 生图 400 | `DOUBAO_IMAGE_MODEL` 是否为 Seedream 模型 ID，非对话 ep |

## 相关文件

- [`.env.example`](./.env.example) — 变量模板
- [`application.properties`](./src/main/resources/application.properties) — 主配置
- [`docs/DEPLOY.md`](../docs/DEPLOY.md) — 生产部署
