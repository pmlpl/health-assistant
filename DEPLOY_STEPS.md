# 快速部署步骤

## 已完成准备工作
✅ 前端代码已打包完成（`frontend/dist`）
✅ 后端 JAR 包已生成（`backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar`）
✅ SQL 清理脚本已创建（`backend/db/migration/clean_invalid_recipes.sql`）
✅ 前端日志已删除

## 手动部署步骤

### 1. 上传文件到服务器
```bash
# 上传 JAR 包
scp backend/target/HealthAssistant-0.0.1-SNAPSHOT.jar root@你的服务器IP:/opt/health-assistant/backend/

# 上传前端文件
scp -r frontend/dist/* root@你的服务器IP:/opt/health-assistant/frontend/dist/

# 上传 SQL 脚本
scp backend/db/migration/clean_invalid_recipes.sql root@你的服务器IP:/tmp/clean_invalid_recipes.sql
```

### 2. 执行数据库清理
```bash
ssh root@你的服务器IP "mysql -u root -p'数据库密码' health_assistant < /tmp/clean_invalid_recipes.sql"
```

### 3. 重启后端服务
```bash
ssh root@你的服务器IP << 'ENDSSH'
cd /opt/health-assistant/backend
# 停止旧进程
PID=$(ps aux | grep 'HealthAssistant-0.0.1-SNAPSHOT.jar' | grep -v grep | awk '{print $2}')
if [ ! -z "$PID" ]; then
    kill $PID
    sleep 3
fi
# 启动新服务
nohup java -jar HealthAssistant-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
sleep 5
echo "后端服务已重启"
ENDSSH
```

### 4. 重启 Nginx
```bash
ssh root@你的服务器IP "sudo systemctl restart nginx"
```

### 5. 验证部署
```bash
# 查看后端日志
ssh root@你的服务器IP "tail -f /opt/health-assistant/backend/backend.log"

# 检查 Nginx 状态
ssh root@你的服务器IP "systemctl status nginx"
```

## SQL 清理脚本说明
该脚本将执行以下操作：
1. 统计无图片数据和重复数据数量
2. 删除所有 `image_url` 为 NULL 或空的数据
3. 删除重复数据（保留 ID 最小的记录）
4. 显示清理后的统计数据

## 注意事项
- 请替换"你的服务器 IP"和"数据库密码"为实际值
- 建议在部署前备份数据库
- 清理数据库后无法恢复，请谨慎操作
