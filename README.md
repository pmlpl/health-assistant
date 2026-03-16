# AI健康助手 🏥

一个基于AI技术的智能健康管理平台，帮助用户记录饮食、分析营养、获得个性化健康建议。

## ️ 运行环境

| 环境 | 版本要求 |
|------|---------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.8+ |

## � 运行步骤

### 1. 数据库配置
编辑 `backend/src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/health_assistant?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

### 2. 启动后端
```bash
cd backend
./mvnw spring-boot:run
```

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### 4. 访问应用
打开浏览器访问：`http://localhost:5173`

## 📚 文档

- **[项目结构说明](./PROJECT_STRUCTURE.md)** - 完整的目录结构和文件说明
- **[快速启动指南](./QUICK_START.md)** - 详细的启动步骤和环境检查
- **[服务器部署说明](./DEPLOY_INSTRUCTIONS.md)** - 生产环境部署指南
- **[后端 API 配置指南](./backend/ENV_SETUP_GUIDE.md)** - AI API Key 和数据库配置
- **[前端 API 配置指南](./frontend/API_CONFIGURATION_GUIDE.md)** - 前端 API 配置原理说明

## 🔧 环境配置

项目使用环境变量管理 API 地址：

- **开发环境**: 自动代理到 `http://localhost:8080`
- **生产环境**: 在 `frontend/.env.production` 中配置后端地址

详细配置请查看上述文档。

## 👤 测试账号

| 账号 | 密码 |
|------|------|
| testuser | 123456 |

## 📄 许可证

MIT License
