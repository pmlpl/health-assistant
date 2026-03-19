# 密钥配置指南

由于特殊原因不能使用系统环境变量来传入 AI 的 API Key，本项目使用 `.env` 文件来管理敏感密钥。

## 配置步骤

### 1. 创建 .env 文件

在 `backend/` 目录下创建 `.env` 文件：

```bash
cd backend
cp .env.example .env
```

### 2. 填写您的 API Key

编辑 `.env` 文件，填入您实际的 API Key：

```env
# 豆包 AI API Key - 用于食物识别和营养分析
DOUBAO_API_KEY=your_actual_doubao_api_key_here

# 通义千问 API Key - 用于 AI 营养咨询
DASHSCOPE_API_KEY=your_actual_dashscope_api_key_here

# MySQL 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=health_assistant
DB_USERNAME=root
DB_PASSWORD=your_password_here
```

### 3. 获取 API Key

#### 豆包 AI (Doubao) API Key
- 访问火山引擎方舟大模型平台
- 注册并创建应用
- 获取 API Key

#### 通义千问 (Qwen/Dashscope) API Key
- 访问阿里云 DashScope 平台
- 注册并开通服务
- 获取 API Key

## 安全注意事项

⚠️ **重要提示**：

1. **不要将 `.env` 文件提交到版本控制**
   - `.env` 文件已经被添加到 `.gitignore` 中
   - 只有 `.env.example` 模板文件可以被提交

2. **保护您的 API Key**
   - 不要与他人分享您的 `.env` 文件
   - 定期更换 API Key
   - 如果怀疑 API Key 泄露，立即在相应平台重新生成

3. **生产环境部署**
   - 在生产环境中，建议使用真正的系统环境变量
   - 或者使用专门的密钥管理服务（如 AWS Secrets Manager、HashiCorp Vault 等）

## 本地开发运行

### 方式一：直接运行（推荐）

Spring Boot 会自动读取 `.env` 文件中的配置：

```bash
cd backend
mvn spring-boot:run
```

### 方式二：手动加载环境变量

如果需要手动加载 `.env` 文件：

#### Windows PowerShell:
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)\s*=\s*(.*)\s*$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
mvn spring-boot:run
```

#### macOS/Linux:
```bash
set -a
source .env
set +a
mvn spring-boot:run
```

## 验证配置

启动应用后，检查日志确认配置是否正确加载：

```
2024-01-01 12:00:00.000  INFO 12345 --- [           main] c.e.h.s.DoubaoFoodRecognitionService     : 豆包 API 密钥已配置
2024-01-01 12:00:00.000  INFO 12345 --- [           main] c.e.h.service.QwenAIService             : Qwen API 密钥已配置
```

## 故障排查

### 问题：API 调用失败，提示密钥未配置

**解决方案：**
1. 检查 `.env` 文件是否存在于 `backend/` 目录
2. 确认 `.env` 文件中的 API Key 是否正确填写
3. 重启应用确保配置重新加载
4. 检查是否有空格或特殊字符导致解析失败

### 问题：数据库连接失败

**解决方案：**
1. 检查 `.env` 文件中的数据库配置是否正确
2. 确认 MySQL 服务是否运行
3. 验证数据库用户名和密码是否正确
4. 检查数据库 `health_assistant` 是否存在

## 文件说明

- `.env` - 实际的环境配置文件（不包含在版本控制中）
- `.env.example` - 配置模板文件（包含在版本控制中）
- `application.properties` - Spring Boot 主配置文件，引用 `.env` 中的变量

## 其他配置项

如果需要添加更多配置项，可以在 `.env` 文件中添加：

```env
# 示例：服务器端口
SERVER_PORT=8080

# 示例：日志级别
LOGGING_LEVEL_ROOT=INFO

# 示例：其他服务的 API Key
OTHER_SERVICE_KEY=your_key_here
```

然后在 `application.properties` 中引用：

```properties
server.port=${SERVER_PORT:8080}
logging.level.root=${LOGGING_LEVEL_ROOT:INFO}
```
