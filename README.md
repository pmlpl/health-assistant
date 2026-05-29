# AI 健康助手

基于 Vue 3 + Spring Boot 3 的智能健康管理平台，支持饮食记录、健身追踪、营养分析、AI 咨询、食谱推荐与健康报告。

## 功能概览

| 模块 | 说明 |
|------|------|
| 首页仪表盘 | 今日概览、喝水打卡、健康提示 |
| 饮食日记 | 餐食记录、拍照识食、营养统计 |
| 健身记录 | 运动类型、时长、消耗热量 |
| 健康食谱 | AI 推荐、手动创建、配图搜索/生图 |
| 健康报告 | 推荐营养目标、近 7 日热量与宏量营养素趋势 |
| 健康日历 | 按日查看饮食与健身汇总 |
| AI 精灵 | 营养咨询、放松引导、健康问卷（支持多模型 BYOK） |
| 个人中心 | 档案管理、AI 设置（文本/视觉/配图服务商） |
| 使用手册 | 应用内 `/guide` 功能说明 |

## 快速开始

详细步骤见 [docs/QUICK_START.md](docs/QUICK_START.md)。

```powershell
# 1. 复制并填写 backend/.env（见 backend/.env.example）
cd backend
Copy-Item .env.example .env

# 2. 启动后端
.\mvnw.cmd spring-boot:run

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/test/health

## 文档索引

| 文档 | 说明 |
|------|------|
| [docs/QUICK_START.md](docs/QUICK_START.md) | 本地开发环境搭建与常见问题 |
| [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md) | 目录结构与模块说明 |
| [docs/DEPLOY.md](docs/DEPLOY.md) | **唯一**生产部署参考（Nginx / Netlify / Railway） |
| [docs/REDESIGN-PLAN.md](docs/REDESIGN-PLAN.md) | 网站外观改版计划 |
| [backend/ENV_SETUP_GUIDE.md](backend/ENV_SETUP_GUIDE.md) | 后端环境变量与 AI 配置 |
| [frontend/API_CONFIGURATION_GUIDE.md](frontend/API_CONFIGURATION_GUIDE.md) | 前端 API 与环境变量 |

> 历史部署文档（`DEPLOY_INSTRUCTIONS.md`、`DEPLOY_STEPS.md`）已合并进 `docs/DEPLOY.md` 并删除。

## 技术栈

- **前端**：Vue 3、Vite、Pinia、Element Plus、ECharts、Axios + JWT
- **后端**：Java 21、Spring Boot 3、Spring Security、JPA、MySQL、Flyway
- **AI**：DeepSeek（平台默认文本）、豆包（识食/生图）、通义千问、本地 LM Studio（BYOK）

## 测试账号（开发环境）

| 账号 | 密码 |
|------|------|
| testuser | 123456 |

生产环境请设置 `APP_SEED_TEST_USER=false`。

## License

MIT
