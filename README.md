# AI 健康助手

基于 Vue 3 + Spring Boot 3 的智能健康管理平台，支持饮食记录、营养分析、AI 咨询与食谱推荐。

## 快速开始

详细说明见 [docs/QUICK_START.md](docs/QUICK_START.md)。

```powershell
# 1. 配置 backend/.env（数据库密码、JWT_SECRET、AI Key）
# 2. 启动后端
cd backend
.\mvnw.cmd spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

- 前端：http://localhost:5173
- 后端：http://localhost:8080

## 文档索引

| 文档 | 说明 |
|------|------|
| [docs/QUICK_START.md](docs/QUICK_START.md) | 本地开发环境搭建 |
| [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md) | 目录与模块说明 |
| [docs/DEPLOY.md](docs/DEPLOY.md) | 生产部署指南 |
| [backend/ENV_SETUP_GUIDE.md](backend/ENV_SETUP_GUIDE.md) | 后端环境变量 |
| [frontend/API_CONFIGURATION_GUIDE.md](frontend/API_CONFIGURATION_GUIDE.md) | 前端 API 配置 |

## 技术栈

- **前端**：Vue 3、Vite、Pinia、Element Plus、ECharts、Axios + JWT
- **后端**：Java 21、Spring Boot 3、Spring Security、JPA、MySQL、Flyway
- **AI**：通义千问（DashScope）、豆包（Volcengine）

## 测试账号（开发环境）

| 账号 | 密码 |
|------|------|
| testuser | 123456 |

生产环境请设置 `APP_SEED_TEST_USER=false`。

## License

MIT
