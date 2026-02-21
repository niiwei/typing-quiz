# Typing Quiz 系统架构设计

本文档描述 Typing Quiz 项目的技术架构、设计决策和核心实现细节。

## 1. 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.18 |
| 数据持久化 | Spring Data JPA | - |
| 数据库 | MySQL | 8.0 |
| 构建工具 | Maven | 3.6+ |
| Java 版本 | Java | 11 |
| 前端 | Vanilla JavaScript + HTML5 + CSS3 | ES6+ |
| 认证 | JWT (JJWT) | - |
| 密码加密 | BCrypt (Spring Security) | - |

## 2. 项目结构

```
src/main/java/com/typingquiz/
├── TypingQuizApplication.java    # 应用入口
├── config/                       # 配置类
│   ├── DataInitializer.java     # 数据初始化
│   └── SecurityConfig.java       # Spring Security 配置
├── entity/                        # 实体类
│   ├── Quiz.java                 # 测验主实体
│   ├── Answer.java               # 答案实体
│   ├── User.java                 # 用户实体
│   ├── QuizGroup.java            # 测验分组实体
│   ├── FillBlankQuiz.java        # 填空题实体
│   └── QuizType.java             # 测验类型枚举
├── repository/                   # 数据访问层
├── service/                      # 业务逻辑层
├── controller/                   # REST API 控制器
├── dto/                          # 数据传输对象
├── exception/                    # 异常处理
└── util/                         # 工具类
```

## 3. 分层架构

```
Controller (REST API) → Service (业务逻辑) → Repository (数据访问) → Entity (数据模型)
```

## 4. 核心实体设计

### Quiz (测验)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 测验标题 |
| description | String | 测验描述 |
| timeLimit | Integer | 时间限制（秒）|
| userId | Long | 创建者ID（账户隔离）|
| quizType | Enum | 测验类型（TYPING/FILL_BLANK）|
| createdAt | LocalDateTime | 创建时间 |
| answers | List<Answer> | 关联答案（一对多）|
| groups | List<QuizGroup> | 关联分组（多对多）|

### Answer (答案)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| quiz | Quiz | 所属测验 |
| content | String | 原始内容 |
| normalizedContent | String | 标准化内容（小写）|
| comment | String | 答案注释 |

### User (用户)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名（唯一）|
| email | String | 邮箱（唯一）|
| password | String | 加密密码 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

### QuizGroup (测验分组)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 分组名称 |
| description | String | 分组描述 |
| userId | Long | 所有者ID |
| displayOrder | Integer | 显示顺序 |
| quizzes | List<Quiz> | 关联测验 |

## 5. 安全设计

### JWT 认证

- Token 有效期: 24小时
- 签名算法: HS256
- 前端请求需携带: `Authorization: Bearer <token>`

### 账户数据隔离

所有数据查询必须按 `userId` 过滤，确保用户只能访问自己的数据。

### 密码加密

使用 Spring Security BCrypt 进行密码加密存储。

## 6. 数据库表结构

```
users (1) ────< (N) quiz (1) ────< (N) answer
    │
    └────< (N) quiz_group (M) ────> (N) quiz
```

**主要表：**
- `users` - 用户表
- `quiz` - 测验表
- `answer` - 答案表
- `quiz_group` - 分组表
- `quiz_group_quiz` - 分组-测验关联表
- `quiz_review_status` - 复习状态表
- `fill_blank_quiz` - 填空题表

## 7. 关键设计决策

| 决策 | 说明 |
|------|------|
| 分层架构 | Controller → Service → Repository → Entity |
| JPA 数据访问 | 使用 Spring Data JPA 自动生成 CRUD |
| 账户隔离 | 所有查询按 userId 过滤 |
| JWT 无状态认证 | 服务端无需存储 Session |
| BCrypt 密码加密 | 防彩虹表攻击 |
| 标准化答案匹配 | normalizedContent 实现大小写不敏感 |
| 级联删除 | 测验删除时自动删除关联答案 |

## 8. 配置管理

### application.properties

```properties
# 服务器配置
server.port=8080

# MySQL 数据库配置
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/typing_quiz
spring.datasource.username=typingquiz
spring.datasource.password=***

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MYSQL_HOST | localhost | MySQL 服务器地址 |

## 9. 部署架构

```
前端 (浏览器) → Spring Boot 应用 (8080) → MySQL 8.0 (3306)
```

**Docker 部署：**
```bash
docker build -t typing-quiz-app .
docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app
```
