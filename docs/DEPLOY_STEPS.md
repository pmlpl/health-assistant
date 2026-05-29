# AI 健康助手 - 服务器部署步骤

## 📦 本地打包完成

JAR 包已生成：`backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar`

---

## 🚀 服务器部署流程

### 方式一：使用 SCP 上传（推荐）

#### 1. 上传 JAR 包到服务器
```bash
# Windows PowerShell / Linux Terminal
scp "C:\Users\pm199\Desktop\传智杯AIWeb开发\AIHealthAssistant\backend\target\HealthAssistant-0.0.1-SNAPSHOT.jar" root@43.99.70.206:/opt/health-assistant/backend/
```

#### 2. SSH 登录服务器
```bash
ssh root@43.99.70.206
```

#### 3. 停止旧服务
```bash
# 查找进程 ID
ps aux | grep HealthAssistant

# 杀死进程（替换<PID>为实际进程号）
kill -9 <PID>

# 或者直接使用 pkill
pkill -f 'HealthAssistant-0.0.1-SNAPSHOT.jar'
```

#### 4. 启动新服务
```bash
cd /opt/health-assistant/backend

# 后台启动（日志输出到 nohup.out）
nohup java -jar HealthAssistant-0.0.1-SNAPSHOT.jar > nohup.out 2>&1 &

# 或者使用 systemd（如果配置了服务）
systemctl restart health-assistant
```

#### 5. 查看启动日志
```bash
# 实时查看日志
tail -f nohup.out

# 或者使用 journalctl（如果使用 systemd）
journalctl -u health-assistant -f
```

#### 6. 重启 Nginx
```bash
# 重新加载 Nginx 配置
nginx -s reload

# 或者重启 Nginx 服务
systemctl restart nginx
```

#### 7. 验证服务状态
```bash
# 检查进程是否运行
ps aux | grep HealthAssistant

# 检查端口监听（后端通常运行在 8080 端口）
netstat -tlnp | grep 8080

# 测试 API 接口
curl http://localhost:8080/api/health
```

---

### 方式二：直接在服务器上拉取代码并编译

如果您已经在服务器上配置了 Git 和 Maven：

#### 1. SSH 登录服务器
```bash
ssh root@43.99.70.206
```

#### 2. 进入项目目录并拉取最新代码
```bash
cd /opt/health-assistant
git pull origin main
```

#### 3. 重新编译打包
```bash
cd backend
mvn clean package -DskipTests
```

#### 4. 重启服务
```bash
# 停止旧服务
pkill -f 'HealthAssistant-0.0.1-SNAPSHOT.jar'

# 启动新服务
nohup java -jar target/HealthAssistant-0.0.1-SNAPSHOT.jar > nohup.out 2>&1 &

# 查看日志
tail -f nohup.out
```

#### 5. 重启 Nginx
```bash
nginx -s reload
```

---

## ✅ 验证部署成功

### 1. 检查后端服务
访问：http://43.99.70.206/api/recipes/my-ai-recipes

### 2. 检查前端页面
访问：http://43.99.70.206/

### 3. 测试 AI 食谱保存功能
1. 打开健康食谱页面
2. 点击"AI 智能生成健康食谱"
3. 选择一个食谱，点击"添加到我的食谱"
4. 应该显示"已将【食谱名称】添加到您的食谱（已永久保存）"
5. 查看控制台日志，应该能看到："✅ 食谱已保存到数据库"

---

## 🔧 Flyway 数据库迁移

首次部署时，Flyway 会自动执行数据库迁移脚本：
- `V20260317__expand_image_storage.sql` - 扩展 image_url 字段为 TEXT 类型

如果自动迁移失败，可以手动执行：
```sql
ALTER TABLE recipe MODIFY COLUMN image_url TEXT;
CREATE INDEX idx_created_by ON recipe(created_by);
CREATE INDEX idx_is_public ON recipe(is_public);
```

---

## 📝 注意事项

1. **确保服务器防火墙开放相应端口**
   - 后端服务端口（默认 8080）
   - HTTP 端口（80）
   - HTTPS 端口（443）

2. **检查 Java 版本**
   ```bash
   java -version
   # 应该是 Java 21+
   ```

3. **确保有足够的磁盘空间**
   ```bash
   df -h
   ```

4. **监控内存使用**
   ```bash
   free -m
   ```

5. **如果遇到权限问题**
   ```bash
   chmod +x /opt/health-assistant/backend/*.sh
   chown -R root:root /opt/health-assistant
   ```

---

## 🆘 故障排查

### 服务启动失败
```bash
# 查看详细错误日志
tail -f /opt/health-assistant/backend/nohup.out

# 检查端口占用
lsof -i :8080
```

### 图片无法保存
1. 检查数据库连接
2. 确认 `image_url` 字段已改为 TEXT 类型
3. 查看后端日志中的 Base64 解码错误

### Nginx 代理失效
```bash
# 检查 Nginx 配置
nginx -t

# 重新加载配置
nginx -s reload
```

---

## 📊 本次更新内容

✅ 新增功能：
- AI 生成的食谱图片永久保存到数据库
- 双重保存机制（数据库 + localStorage）
- 用户提示"已永久保存"

✅ 技术改进：
- 扩展 database 字段支持大尺寸 Base64 图片
- 新增 `/api/recipes/save-ai-generated` 接口
- 优化用户体验，图片不会因缓存清理而丢失
