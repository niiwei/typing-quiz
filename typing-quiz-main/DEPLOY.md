# 安全清理后的部署指南

> 本文档指导你如何在代码清理敏感信息后配置和部署项目

---

## 快速开始（本地开发）

### 1. 创建环境变量文件

```bash
# 复制模板文件
cp .env.example .env

# 编辑 .env 文件，填写你的真实配置
notepad .env  # Windows
# 或
nano .env      # Linux/Mac
```

### 2. 填写环境变量

**.env 文件示例：**
```bash
# 数据库配置（本地开发可填 localhost，远程填服务器IP）
MYSQL_HOST=your_server_ip
MYSQL_USER=typingquiz
MYSQL_PASSWORD=your_actual_password

# JWT密钥（建议使用随机生成的32位以上字符串）
JWT_SECRET=your_random_jwt_secret_key_here
```

### 3. 启动应用

**Windows:**
```bash
# 方式1: 双击启动（自动加载.env）
start.bat

# 方式2: PowerShell
.\start.ps1
```

**Linux/Mac:**
```bash
# 加载环境变量后启动
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

---

## 服务器部署（Docker）

### 方式1: Docker Compose（推荐）

```bash
# 1. 在服务器上创建 .env 文件
cat > .env << EOF
MYSQL_HOST=localhost
MYSQL_USER=typingquiz
MYSQL_PASSWORD=your_production_password
JWT_SECRET=your_production_jwt_secret
EOF

# 2. 启动应用
docker-compose up -d

# 3. 查看日志
docker-compose logs -f
```

### 方式2: 纯 Docker 命令

```bash
# 构建镜像
docker build -t typing-quiz-app .

# 运行容器（传入环境变量）
docker run -d \
  --network host \
  --name typing-quiz-app \
  -e MYSQL_HOST=localhost \
  -e MYSQL_PASSWORD=your_password \
  -e JWT_SECRET=your_secret \
  -v ./data:/app/data \
  typing-quiz-app
```

---

## 自动部署脚本

**前提：** 配置好SSH免密登录

```powershell
# 本地一键部署到服务器
ssh root@your_server_ip "cd /app/typing-quiz && git pull && docker-compose up -d --build"
```

---

## 环境变量说明

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| MYSQL_HOST | 是 | localhost | MySQL服务器地址 |
| MYSQL_USER | 否 | typingquiz | MySQL用户名 |
| MYSQL_PASSWORD | 是 | - | MySQL密码（必须设置） |
| JWT_SECRET | 是 | - | JWT签名密钥（至少32位） |
| SERVER_PORT | 否 | 8080 | 应用端口 |

---

## 生成安全密钥

**生成随机JWT密钥：**
```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | % {[char]$_})
```

---

## 常见问题

### Q1: 启动时提示环境变量未设置
**解决：** 确保 .env 文件存在且格式正确，每行格式为 `KEY=VALUE`

### Q2: Docker容器无法连接数据库
**解决：** 检查 MYSQL_HOST 设置：
- 数据库在宿主机：`MYSQL_HOST=localhost`
- 数据库在容器内：`MYSQL_HOST=mysql_container_name`

### Q3: 如何修改已部署应用的环境变量
```bash
# 停止容器
docker-compose down

# 修改 .env 文件
nano .env

# 重新启动
docker-compose up -d
```

---

## 安全提示

1. **永远不要将 .env 文件提交到Git**
   - 已添加到 .gitignore

2. **生产环境使用强密码**
   - MySQL密码：至少16位随机字符串
   - JWT密钥：32位以上base64编码

3. **定期更换密钥**
   - JWT密钥泄露会导致所有Token失效风险
   - 更换后所有用户需要重新登录

4. **服务器安全**
   - 关闭不必要的端口
   - 使用防火墙限制MySQL访问
   - 配置SSH密钥登录（禁用密码登录）

---

**最后更新：** 2026-04-02
