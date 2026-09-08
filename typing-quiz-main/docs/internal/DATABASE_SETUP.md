# 数据库配置说明

## MySQL 数据库信息

| 配置项 | 值 |
|--------|-----|
| 数据库类型 | MySQL 8.0 |
| 服务器地址 | your_server_ip |
| 端口 | 3306 |
| 数据库名 | typing_quiz |
| 字符集 | utf8mb4_unicode_ci |

## 用户账户说明

### 1. root 用户（管理员）
- 用户名：`root`
- 密码：`your_mysql_password`
- 用途：数据库管理（创建数据库、用户、授权）
- 说明：给人用的管理员账户

### 2. typingquiz 用户（应用连接）
- 用户名：`typingquiz`
- 密码：`your_mysql_password`
- 用途：应用代码连接数据库
- 说明：**不是给人登录的**，是 Java 应用连接 MySQL 时用的

### 3. 注册用户（应用账户）
- 用途：用户登录应用使用
- 创建方式：通过注册页面 http://your_server_ip:8080/register.html
- 说明：保存在 `users` 表中，每个注册的用户都是独立的

## 比喻说明

| 用户类型 | 比喻 |
|----------|------|
| root | 餐厅老板（管理整个店） |
| typingquiz | 餐厅服务员（拿数据给顾客） |
| 注册用户 | 顾客（来吃饭/使用应用） |

## 连接信息

## 云服务器部署路径

- 项目目录：`/app/typing-quiz`
- 常用进入方式：`cd /app/typing-quiz`

### 应用配置（application.properties）
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/typing_quiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=typingquiz
spring.datasource.password=your_mysql_password
```

### 远程连接示例
```bash
mysql -h your_server_ip -u typingquiz -p typing_quiz
```

## 注意事项

1. **root 用户**允许远程登录，方便在其他电脑管理数据库
2. **typingquiz 用户**用于应用连接，不要公开密码
3. 生产环境建议更换更复杂的密码
