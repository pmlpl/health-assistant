# 项目结构说明

## 目录结构

```
health-assistant/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/example/healthassistant/
│   │   ├── ai/                 # AI 客户端（DeepSeek/通义/豆包/LM Studio/流式）
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
│   ├── .env.example            # 环境变量模板（含 DEEPSEEK、AI_PROVIDER_DEFAULT 等）
│   └── ENV_SETUP_GUIDE.md
│
├── frontend/                   # Vue 3 前端（Bauhaus UI）
│   ├── src/
│   │   ├── api/                # healthApi、questionnaireApi
│   │   ├── assets/styles/      # tokens.css、design-system.css、global.css
│   │   ├── components/
│   │   │   ├── layout/         # Header、Navigation、Footer
│   │   │   ├── dashboard/      # 图表、快捷操作
│   │   │   ├── common/         # ApiStatusIndicator、LoadingSpinner
│   │   │   └── AISpirit.vue    # 全局 AI 精灵
│   │   ├── composables/        # useAIConsult、usePageMotion 等
│   │   ├── constants/          # aiProviders、aiPageJobIds
│   │   ├── router/             # 路由
│   │   ├── services/           # aiStreamRunner、aiPageJobRunner
│   │   ├── stores/             # userStore、aiConsultStore、aiPageJobsStore
│   │   ├── utils/              # notify、pageDraftStorage、motion
│   │   └── views/              # 页面视图
│   ├── .env.example            # VITE_API_BASE_URL 模板
│   ├── vite.config.js          # dev 代理 /api、/uploads → localhost:8080
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

## 前端设计系统（Bauhaus）

| 文件 | 说明 |
|------|------|
| `frontend/src/assets/styles/tokens.css` | 色板、间距、阴影等设计 Token |
| `frontend/src/assets/styles/design-system.css` | 组件样式（`.bauhaus-block`、`.bh-card` 等） |
| `frontend/src/assets/styles/global.css` | 全局样式入口 |

主色：红 `#D02020`、蓝 `#1040C0`、黄 `#F0C020`；硬边 2px 边框 + 偏移阴影。各页面（Dashboard、Diary、Fitness、Guide 等）使用 `.bauhaus-block--red/blue/yellow` 色块分区。

## 前端路由

| 路径 | 页面 | 功能 |
|------|------|------|
| `/` | DashboardView | 首页仪表盘 |
| `/diary` | DiaryView | 饮食日记 |
| `/fitness` | FitnessView | 健身记录（多选维度 + 距离） |
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
| `DoubaoFoodRecognitionService` | 豆包识食实现 |
| `RecipeImageSearchService` | Pexels + 豆包生图 + 同名复用 |
| `PlatformAiQuotaService` | 平台试用配额 |

## 数据模型要点

### FitnessRecord（健身记录）

| 字段 | 说明 |
|------|------|
| `type` | 运动大类（有氧、力量等） |
| `durationMinutes` | 时长（分钟） |
| `repetitions` | 次数 |
| `weightKg` | 重量（kg） |
| `distanceKm` | 距离（公里，跑步等有氧） |
| `calories` | 消耗热量 |

前端支持 **记录维度多选**（时长 / 次数 / 重量 / 距离），后端按非空字段持久化。

## 配置层级

### 后端

```
backend/.env → EnvConfig → application*.properties → Spring Boot
```

优先级：`.env` > 系统环境变量 > properties 默认值

**AI 默认**：`AI_PROVIDER_DEFAULT=deepseek`，需 `DEEPSEEK_API_KEY`；LM Studio 为用户 BYOK 选项。

### 前端

```
.env.development / .env.production → import.meta.env → healthApi.js
```

开发环境 Vite 将 `/api` 代理到 `localhost:8080`；默认 dev 端口 **5173**（占用时 **5174**）。

## 文档索引

| 文档 | 用途 |
|------|------|
| [README.md](../README.md) | 项目总览 |
| [QUICK_START.md](./QUICK_START.md) | 本地启动 |
| [DEPLOY.md](./DEPLOY.md) | 生产部署 |
| [backend/.env.example](../backend/.env.example) | 环境变量模板 |
| [backend/ENV_SETUP_GUIDE.md](../backend/ENV_SETUP_GUIDE.md) | 后端环境变量 |
| [frontend/API_CONFIGURATION_GUIDE.md](../frontend/API_CONFIGURATION_GUIDE.md) | 前端 API |

## 注意事项

- 勿将 `backend/.env`、`frontend/.env.production` 提交 Git
- 用户上传文件在 `uploads/`，生产需配置 `APP_UPLOAD_DIR` 并持久化
- 生产环境关闭 `APP_SEED_TEST_USER`
- 设计计划类文档（`*PLAN*.md`）为本地草稿，已在 `.gitignore` 中排除
