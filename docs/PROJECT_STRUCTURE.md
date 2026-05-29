# 项目结构说明

## 目录结构

```
health-assistant/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/example/healthassistant/
│   │   ├── ai/                 # AI 客户端（DashScope/豆包/LM Studio/流式）
│   │   ├── config/             # 安全、CORS、环境变量、SSE HTTP
│   │   ├── controller/         # REST API
│   │   ├── dto/                # 请求/响应 DTO
│   │   ├── model/              # JPA 实体
│   │   ├── repository/         # 数据访问
│   │   ├── service/            # 业务逻辑
│   │   ├── security/           # JWT 认证
│   │   └── util/               # 图片处理等工具
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   ├── application-prod.properties
│   │   └── db/migration/       # Flyway 脚本
│   ├── .env.example
│   └── ENV_SETUP_GUIDE.md
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/                # healthApi、questionnaireApi
│   │   ├── components/
│   │   │   ├── layout/         # Header、Navigation、Footer
│   │   │   ├── dashboard/      # 图表、快捷操作
│   │   │   ├── common/         # ApiStatusIndicator、LoadingSpinner
│   │   │   └── AISpirit.vue    # 全局 AI 精灵
│   │   ├── composables/        # useAIConsult 等
│   │   ├── constants/          # aiProviders、aiPageJobIds
│   │   ├── router/             # 路由
│   │   ├── services/           # aiStreamRunner、aiPageJobRunner
│   │   ├── stores/             # userStore、aiConsultStore、aiPageJobsStore
│   │   ├── utils/              # notify、pageDraftStorage
│   │   └── views/              # 页面视图
│   ├── vite.config.js
│   └── API_CONFIGURATION_GUIDE.md
│
├── docs/
│   ├── QUICK_START.md
│   ├── DEPLOY.md               # 唯一部署文档
│   └── PROJECT_STRUCTURE.md    # 本文件
│
├── netlify.toml                # Netlify SPA 回退
└── README.md
```

## 前端路由

| 路径 | 页面 | 功能 |
|------|------|------|
| `/` | DashboardView | 首页仪表盘 |
| `/diary` | DiaryView | 饮食日记 |
| `/fitness` | FitnessView | 健身记录 |
| `/recipes` | RecipesView | 健康食谱 |
| `/health-report` | HealthReportView | 营养目标与 7 日报告 |
| `/calendar` | CalendarView | 健康日历 |
| `/profile` | ProfileView | 个人档案与 AI 设置 |
| `/guide` | UserGuideView | 使用手册 |
| `/ai-recipe-result` | AIRecipeResultView | AI 食谱结果（新标签） |
| `/login` `/register` | 登录注册 | 访客页 |

全局组件 **AISpirit** 在 `App.vue` 挂载，各页可用。

## 后端核心模块

| 模块 | 说明 |
|------|------|
| `HealthAiService` | 营养咨询、食谱生成、prompt 构建 |
| `ChatClientRouter` | 按用户设置路由到 DeepSeek/通义/豆包/LM Studio |
| `UserAiSettingsService` | 用户 AI 设置加密存储与解析 |
| `AiConnectionTestService` | 连接测试（与对话路径应对齐） |
| `ImageRecognitionService` | 拍照识食 |
| `RecipeImageSearchService` | Pexels + 豆包生图 + 同名复用 |
| `PlatformAiQuotaService` | 平台试用配额 |

## 配置层级

### 后端

```
backend/.env → EnvConfig → application*.properties → Spring Boot
```

优先级：`.env` > 系统环境变量 > properties 默认值

### 前端

```
.env.development / .env.production → import.meta.env → healthApi.js
```

开发环境 Vite 将 `/api` 代理到 `localhost:8080`。

## 文档索引

| 文档 | 用途 |
|------|------|
| [README.md](../README.md) | 项目总览 |
| [QUICK_START.md](./QUICK_START.md) | 本地启动 |
| [DEPLOY.md](./DEPLOY.md) | 生产部署 |
| [backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md) | 后端环境变量 |
| [frontend/API_CONFIGURATION_GUIDE.md](../frontend/API_CONFIGURATION_GUIDE.md) | 前端 API |

## 注意事项

- 勿将 `backend/.env`、`frontend/.env.production` 提交 Git
- 用户上传文件在 `uploads/`，生产需配置 `APP_UPLOAD_DIR` 并持久化
- 生产环境关闭 `APP_SEED_TEST_USER`
