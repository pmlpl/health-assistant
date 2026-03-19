# AI健康助手 - 快速启动指南

## 🚀 一键启动（推荐）

### Windows PowerShell
```powershell
# 在项目根目录执行
.\start-dev.ps1
```

### 手动启动

#### 1. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

#### 2. 启动前端服务（新终端窗口）
```bash
cd frontend
npm run dev
```

---

## 📋 环境检查清单

### 启动前检查
- [ ] Node.js 已安装（建议 v18+）
- [ ] Java JDK 已安装（建议 JDK 21+）
- [ ] Maven 已安装
- [ ] MySQL 8.0+ 已安装并运行
- [ ] `.env.development` 文件存在（内容为 `VITE_API_BASE_URL=/api`）
- [ ] 后端 `.env` 文件已配置（包含 AI API Key 和数据库配置）

### 验证服务状态
1. **后端服务**：访问 http://localhost:8080/test/health
2. **前端服务**：访问 http://localhost:5173

---

## 🔧 配置文件说明

### 开发环境（本地）
- **前端地址**: http://localhost:5173
- **后端地址**: http://localhost:8080
- **API 代理**: 自动将 `/api` 请求代理到后端

### 生产环境（部署）
修改 `frontend/.env.production`:
```bash
VITE_API_BASE_URL=https://your-backend-domain.com
```

然后重新构建:
```bash
cd frontend
npm run build
```

---

## ❓ 常见问题

### Q: 启动时提示端口被占用？
**A**: 
```bash
# 方法 1：关闭占用端口的进程
# Windows:
netstat -ano | findstr :5173
taskkill /PID <进程 ID> /F

# 方法 2：修改端口（frontend/vite.config.js）
server: {
  port: 3000  // 改为其他端口
}
```

### Q: API 请求失败？
**A**: 
1. 确认后端服务已启动
2. 检查浏览器控制台查看具体错误
3. 确认 `.env.development` 配置正确
4. 检查后端 `.env` 文件是否配置了 AI API Key

### Q: 如何获取 AI API Key？
**A**: 
1. **豆包 AI**: 访问火山引擎方舟大模型平台 (https://www.volcengine.com/product/ark)
2. **通义千问**: 访问阿里云 DashScope 平台 (https://dashscope.console.aliyun.com/)
3. 详细配置请查看 [后端 API 配置指南](./backend/ENV_SETUP_GUIDE.md)

### Q: 打包后仍然报错？
**A**: 
1. 修改 `frontend/.env.production` 为实际后端地址
2. 删除 `dist` 目录
3. 重新执行 `npm run build`

---

## 📖 更多文档

- [后端 API 配置指南](./backend/ENV_SETUP_GUIDE.md) - AI API Key 和数据库配置
- [前端 API 配置指南](./frontend/API_CONFIGURATION_GUIDE.md) - API 配置原理说明
- [服务器部署说明](./DEPLOY_INSTRUCTIONS.md) - 生产环境部署步骤
- [项目 README](./README.md) - 项目概述
