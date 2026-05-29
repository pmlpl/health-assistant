# AI健康助手 - 项目结构说明

## 📁 目录结构

```
AIHealthAssistant/
├── backend/                 # 后端服务（Spring Boot + Java 21）
│   ├── src/main/
│   │   ├── java/com/example/healthassistant/
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # REST API 控制器
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── model/           # 数据模型
│   │   │   ├── repository/      # 数据访问层
│   │   │   └── service/         # 业务逻辑层
│   │   └── resources/
│   │       ├── application.properties  # Spring Boot 主配置
│   │       └── db/migration/          # 数据库迁移脚本
│   ├── .env                   # 环境变量配置（敏感信息，不提交到 Git）
│   ├── .env.example           # 环境变量模板
│   ├── ENV_SETUP_GUIDE.md     # 后端配置指南
│   └── pom.xml                # Maven 配置
│
├── frontend/                  # 前端应用（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/               # API 调用封装
│   │   ├── assets/            # 静态资源
│   │   │   └── images/        # 图片资源
│   │   ├── components/        # Vue 组件
│   │   ├── composables/       # 组合式函数
│   │   ├── router/            # 路由配置
│   │   ├── services/          # 服务层
│   │   ├── stores/            # 状态管理（Pinia）
│   │   ├── utils/             # 工具函数
│   │   └── views/             # 页面视图
│   ├── .env.development       # 开发环境配置
│   ├── .env.production        # 生产环境配置
│   ├── .env.example           # 环境配置示例
│   ├── API_CONFIGURATION_GUIDE.md  # API 配置指南
│   ├── vite.config.js         # Vite 配置
│   └── package.json           # NPM 依赖
│
├── docs/                      # 项目文档（已精简）
│
├── README.md                  # 项目总览和快速开始
├── QUICK_START.md             # 详细启动指南
└── DEPLOY_INSTRUCTIONS.md     # 服务器部署说明
```

## 🎯 核心文件说明

### 根目录文件
- **`README.md`** - 项目概述、技术栈、测试账号
- **`QUICK_START.md`** - 详细的启动步骤、环境检查、常见问题
- **`DEPLOY_INSTRUCTIONS.md`** - 生产环境部署完整指南
- **`start-dev.ps1`** - Windows 一键启动脚本

### Backend 关键文件
- **`.env`** - 环境变量配置（AI API Key、数据库密码等敏感信息）
- **`.env.example`** - 环境变量模板（可安全分享）
- **`ENV_SETUP_GUIDE.md`** - 后端密钥配置详细指南
- **`application.properties`** - Spring Boot 主配置文件

### Frontend 关键文件
- **`.env.development`** - 开发环境 API 配置（使用 Vite 代理）
- **`.env.production`** - 生产环境 API 配置（完整后端地址）
- **`.env.example`** - 环境配置示例
- **`API_CONFIGURATION_GUIDE.md`** - API 配置原理和使用说明
- **`vite.config.js`** - Vite 开发和代理配置

## 🔑 配置文件层级关系

### 后端配置流程
```
.env 文件 → EnvConfig.java → application.properties → Spring Boot 应用
```

**优先级**：`.env` 文件 > `application.properties` > 系统环境变量

### 前端配置流程
```
.env.* 文件 → import.meta.env → healthApi.js → Axios 请求
```

**环境识别**：Vite 根据运行命令自动加载对应的 `.env` 文件

## 📊 文档索引

| 文档 | 位置 | 用途 |
|------|------|------|
| 项目总览 | `README.md` | 了解项目功能、技术栈、测试账号 |
| 快速启动 | `QUICK_START.md` | 本地开发环境搭建和启动 |
| 后端配置 | `backend/ENV_SETUP_GUIDE.md` | 配置 AI API Key 和数据库 |
| 前端配置 | `frontend/API_CONFIGURATION_GUIDE.md` | 配置 API 地址和环境变量 |
| 服务器部署 | `DEPLOY_INSTRUCTIONS.md` | 生产环境部署步骤 |

## 🚀 快速导航

### 本地开发
1. 查看 [`README.md`](./README.md) 了解项目
2. 查看 [`QUICK_START.md`](./QUICK_START.md) 启动项目
3. 配置后端密钥：[`backend/ENV_SETUP_GUIDE.md`](./backend/ENV_SETUP_GUIDE.md)

### 生产部署
1. 构建前后端项目
2. 查看 [`DEPLOY_INSTRUCTIONS.md`](./DEPLOY_INSTRUCTIONS.md) 部署到服务器

## 💡 提示

- **不要将 `.env` 文件提交到 Git** - 包含敏感信息
- **开发和生产环境使用不同的配置** - 自动切换
- **遇到问题先查看文档** - 大部分问题都有解决方案
