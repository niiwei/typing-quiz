# 部署指南

本文档介绍 敲脑壳 MindPop 项目的部署流程和操作方法。

## 1. 环境准备

### 服务器要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 11+ | 运行环境 |
| MySQL | 8.0 | 数据存储 |
| Docker | 最新版 | 容器化部署 |
| Git | 最新版 | 代码拉取 |

### 配置文件

创建 `application.properties`：

```properties
# 服务器配置
server.port=8080

# MySQL 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/typing_quiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=typingquiz
spring.datasource.password=your_password

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

## 2. 本地部署

### 使用 Maven Wrapper

```bash
# 构建项目
./mvnw clean package

# 运行
java -jar target/typing-quiz-1.1.0.jar
```

### 使用启动脚本

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

## 3. 云端部署（Docker）

### 自动部署（推荐）

已配置 SSH 免密登录，一键部署：

```powershell
ssh -i C:\Users\29982\.ssh\typing_quiz_deploy root@47.102.147.127 "cd /app/typing-quiz && git pull && docker build -t typing-quiz-app . && docker rm -f typing-quiz-app && docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app"
```

### 手动部署步骤

```bash
# 1. SSH 登录服务器
ssh root@47.102.147.127

# 2. 进入项目目录
cd /app/typing-quiz

# 3. 拉取最新代码
git pull

# 4. 构建 Docker 镜像
docker build -t typing-quiz-app .

# 5. 删除旧容器
docker rm -f typing-quiz-app

# 6. 运行新容器
docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app
```

### Docker 部署配置

| 配置项 | 值 |
|--------|-----|
| 服务器地址 | 47.102.147.127 |
| SSH 密钥 | C:\Users\29982\.ssh\typing_quiz_deploy |
| 项目目录 | /app/typing-quiz |
| 容器名称 | typing-quiz-app |
| 端口映射 | 8080 (host 网络模式) |

## 4. 数据库部署

### 4.1 MySQL 初始化

```sql
-- 创建数据库
CREATE DATABASE typing_quiz CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户
CREATE USER 'typingquiz'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON typing_quiz.* TO 'typingquiz'@'%';
FLUSH PRIVILEGES;
```

### 4.2 数据库配置详情

| 配置项 | 值 |
|--------|-----|
| 数据库类型 | MySQL 8.0 |
| 服务器地址 | 47.102.147.127 |
| 端口 | 3306 |
| 数据库名 | typing_quiz |
| 字符集 | utf8mb4_unicode_ci |

**用户账户说明：**

| 用户 | 密码 | 用途 |
|------|------|------|
| root | pianzigunsb123.. | 数据库管理（创建数据库、用户、授权） |
| typingquiz | pianzigunsb123.. | 应用代码连接数据库 |

**应用配置（application.properties）：**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/typing_quiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=typingquiz
spring.datasource.password=pianzigunsb123..
```

**远程连接示例：**
```bash
mysql -h 47.102.147.127 -u typingquiz -p typing_quiz
```

### 4.3 数据库备份

```bash
# 备份
mysqldump -h 47.102.147.127 -u typingquiz -p typing_quiz > backup.sql

# 恢复
mysql -h 47.102.147.127 -u typingquiz -p typing_quiz < backup.sql
```

## 5. 常见问题处理

### 问题1: 云端冲突文件

**现象**：`git pull` 报错 "untracked working tree files would be overwritten"

**解决方案**：
```bash
rm -f src/main/java/com/typingquiz/controller/LearningController.java
rm -f src/main/java/com/typingquiz/controller/ReviewController.java
rm -f src/main/java/com/typingquiz/dto/GroupReviewDTO.java
rm -f src/main/java/com/typingquiz/dto/LearnResponseDTO.java
rm -f src/main/java/com/typingquiz/dto/QuizReviewItemDTO.java
git pull
```

### 问题2: 端口被占用

**解决方案**：
```properties
# 修改 application.properties
server.port=8081
```

### 问题3: 数据库连接失败

**检查清单**：
- [ ] MySQL 服务已启动
- [ ] 数据库已创建
- [ ] 用户名密码正确
- [ ] 防火墙允许 3306 端口

## 6. 部署检查清单

- [ ] 本地代码已提交并推送到 GitHub
- [ ] `pom.xml` 版本号已更新（如有需要）
- [ ] 云端 MySQL 服务正常运行
- [ ] Docker 镜像构建成功
- [ ] 新容器启动无异常
- [ ] 应用端口 8080 可访问

## 7. 回滚方案

### 代码回滚

```bash
# 回滚到上一个版本
git reset --hard HEAD~1
git push --force

# 或回滚到指定标签
git reset --hard v1.1.0
git push --force
```

### 数据回滚

```bash
# 停止应用
docker rm -f typing-quiz-app

# 恢复数据库备份
mysql -h 47.102.147.127 -u typingquiz -p typing_quiz < backup.sql

# 重新启动
docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app
```

## 8. 监控与维护

### 查看日志

```bash
# 实时日志
docker logs -f typing-quiz-app

# 最近100行
docker logs --tail 100 typing-quiz-app
```

### 重启应用

```bash
docker restart typing-quiz-app
```
