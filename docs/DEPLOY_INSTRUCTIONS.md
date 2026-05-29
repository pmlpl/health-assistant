# AI健康助手 - 服务器部署说明

## 📦 部署前准备

### 必需软件
- JDK 21+
- MySQL 8.0+
- Nginx（或其他 Web 服务器）
- Node.js 18+（用于构建前端）
- Maven 3.8+（用于构建后端）

---

## 🚀 本地构建步骤

### 1. 构建前端
```bash
cd frontend

# 修改生产环境配置
# 编辑 .env.production，设置后端地址
VITE_API_BASE_URL=http://你的服务器 IP:8080

# 安装依赖并构建
npm install
npm run build
```

构建成功后会生成 `dist` 目录。

### 2. 构建后端
```bash
cd backend

# 配置数据库连接（application.properties）
# 确保指向生产环境的 MySQL

# 打包项目
mvn clean package
```

构建成功后会在 `target` 目录生成 `HealthAssistant-0.0.1-SNAPSHOT.jar`。

---

## 🚀 部署步骤

### 方法一：手动上传（推荐）

#### 1. 上传后端 JAR 包
```bash
# 在本地执行
scp backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar root@43.99.70.206:/opt/health-assistant/
```

#### 2. 上传前端文件
```bash
# 将 dist 目录所有文件上传到服务器的 Web 服务器目录
# 例如 Nginx 的 /usr/share/nginx/html/
scp frontend/dist/* root@43.99.70.206:/usr/share/nginx/html/
```

#### 3. 在服务器上启动后端
```bash
cd /opt/health-assistant/
nohup java -jar HealthAssistant-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

#### 4. 验证服务
- 前端：http://43.99.70.206
- 后端 API: http://43.99.70.206:8080/test/health

---

### 方法二：使用 PowerShell 脚本一键部署

在项目根目录创建 `deploy-to-server.ps1`，然后执行：
```powershell
.\deploy-to-server.ps1
```

---

## ⚙️ 服务器配置要求

### 必需软件
- JDK 21+
- MySQL 8.0+
- Nginx（或其他 Web 服务器）

### 端口开放
- 80 (HTTP) - 前端访问
- 443 (HTTPS) - 可选
- 8080 (后端 API)

---

## 🔧 Nginx 配置示例

```nginx
server {
    listen 80;
    server_name 43.99.70.206;
    
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端 API 代理（可选，如果需要统一端口）
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_cache_bypass $http_upgrade;
    }
}
```

---

## ✅ 验证清单

- [ ] MySQL 服务已启动
- [ ] 数据库已创建（health_assistant）
- [ ] 后端 JAR 包已上传
- [ ] 后端 `.env` 文件已配置（AI API Key）
- [ ] 前端文件已上传到 Nginx 目录
- [ ] 后端服务已启动（检查 8080 端口）
- [ ] Nginx 服务已启动
- [ ] 可以访问 http://43.99.70.206
- [ ] API 测试成功：http://43.99.70.206:8080/test/health

---

## 🐛 故障排查

### 后端无法启动
```bash
# 查看日志
tail -f app.log

# 检查端口占用
netstat -tulpn | grep 8080
```

### 前端无法访问
```bash
# 检查 Nginx 状态
systemctl status nginx

# 查看 Nginx 错误日志
tail -f /var/log/nginx/error.log
```

### API 请求失败
- 检查防火墙是否开放 8080 端口
- 确认 `.env.production` 中的 API 地址正确
- 查看浏览器控制台的网络请求错误
