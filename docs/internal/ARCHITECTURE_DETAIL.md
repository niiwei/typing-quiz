# 敲脑壳 MindPop 系统架构设计

> 本文档描述 敲脑壳 MindPop 项目的技术架构、设计决策和核心实现细节。

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

---

## 2. 项目结构

```
src/main/java/com/typingquiz/
├── TypingQuizApplication.java    # 应用入口
├── config/                       # 配置类
│   ├── DataInitializer.java     # 数据初始化
│   └── SecurityConfig.java       # Spring Security 配置
├── entity/                        # 实体类 (数据模型)
│   ├── Quiz.java                 # 测验主实体
│   ├── Answer.java               # 答案实体
│   ├── User.java                 # 用户实体
│   ├── QuizGroup.java            # 测验分组实体
│   ├── FillBlankQuiz.java        # 填空题实体
│   └── QuizType.java             # 测验类型枚举
├── repository/                   # 数据访问层 (Spring Data JPA)
│   ├── QuizRepository.java
│   ├── AnswerRepository.java
│   ├── UserRepository.java
│   ├── QuizGroupRepository.java
│   └── FillBlankQuizRepository.java
├── service/                      # 业务逻辑层
│   ├── QuizService.java          # 测验核心业务
│   ├── AnswerService.java        # 答案验证业务
│   ├── UserService.java          # 用户管理业务
│   ├── QuizGroupService.java     # 分组管理业务
│   └── FillBlankQuizService.java # 填空题业务
├── controller/                   # REST API 控制器
│   ├── QuizController.java       # 测验 CRUD API
│   ├── AuthController.java       # 认证 API
│   ├── ImportExportController.java # 导入导出 API
│   ├── DatabaseController.java  # 数据库管理 API
│   ├── QuizGroupController.java  # 分组 API
│   └── HomeController.java       # 页面路由
├── dto/                          # 数据传输对象
├── exception/                    # 异常处理
└── util/                         # 工具类
    └── JwtUtil.java             # JWT 工具
```

---

## 3. 分层架构

```
┌─────────────────────────────────────────────────────┐
│                   Controller 层                     │
│              (REST API 端点处理)                     │
├─────────────────────────────────────────────────────┤
│                   Service 层                         │
│              (业务逻辑处理)                          │
├─────────────────────────────────────────────────────┤
│                 Repository 层                       │
│              (数据访问抽象)                          │
├─────────────────────────────────────────────────────┤
│                   Entity 层                         │
│              (数据模型定义)                          │
├─────────────────────────────────────────────────────┤
│                   Database                           │
│                 (MySQL 8.0)                          │
└─────────────────────────────────────────────────────┘
```

---

## 4. 核心实体设计

### 4.1 Quiz (测验)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 测验标题 |
| description | String | 测验描述 |
| timeLimit | Integer | 时间限制（秒） |
| userId | Long | 创建者ID（账户隔离） |
| quizType | Enum | 测验类型（TYPING/FILL_BLANK） |
| createdAt | LocalDateTime | 创建时间 |
| answers | List<Answer> | 关联答案（一对多） |
| groups | List<QuizGroup> | 关联分组（多对多） |

**关系：**
- `Quiz` → `Answer` (OneToMany, cascade=ALL, orphanRemoval)
- `Quiz` → `QuizGroup` (ManyToMany)

---

### 4.2 Answer (答案)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| quiz | Quiz | 所属测验（ManyToOne） |
| content | String | 原始内容 |
| normalizedContent | String | 标准化内容（小写） |
| comment | String | 答案注释 |

**设计特点：**
- `normalizedContent` 用于大小写不敏感匹配
- `comment` 用于答案备注（答题时不参与匹配）
- 索引：`idx_quiz_id`, `idx_normalized_content`

---

### 4.3 User (用户)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名（唯一） |
| email | String | 邮箱（唯一） |
| password | String | 加密密码 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### 4.4 QuizGroup (测验分组)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 分组名称 |
| description | String | 分组描述 |
| userId | Long | 所有者ID（账户隔离） |
| displayOrder | Integer | 显示顺序 |
| quizzes | List<Quiz> | 关联测验（多对多） |

---

### 4.5 FillBlankQuiz (填空题)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| quizId | Long | 关联测验ID |
| fullText | String | 完整文本 |
| blanksInfo | String | 挖空信息（JSON格式） |
| displayText | String | 显示文本（___替换） |
| blanksCount | Integer | 挖空数量 |

---

## 5. API 端点设计

### 5.1 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |

---

### 5.2 测验相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/quizzes` | 获取所有测验（按用户过滤） |
| GET | `/api/quizzes/{id}` | 获取测验详情 |
| POST | `/api/quizzes` | 创建测验 |
| PUT | `/api/quizzes/{id}` | 更新测验 |
| DELETE | `/api/quizzes/{id}` | 删除测验 |
| GET | `/api/quizzes/{id}/answers` | 获取测验答案 |

---

### 5.3 导入导出相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/import-export/quizzes/export` | 导出所有测验 |
| GET | `/api/import-export/quiz/{id}/export` | 导出单个测验 |
| GET | `/api/import-export/group/{id}/export` | 按分组导出 |
| POST | `/api/import-export/quizzes/import` | 批量导入 |
| POST | `/api/import-export/quiz/import` | 单个导入 |

---

### 5.4 数据库管理相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/database/stats` | 数据库统计 |
| GET | `/api/database/quiz/{id}/answers` | 按测验ID查答案 |
| GET | `/api/database/quiz/search?name=xxx` | 按名称搜测验 |
| GET | `/api/database/answer/{id}/quiz` | 按答案ID查测验 |
| GET | `/api/database/answer/search?content=xxx` | 按内容搜答案 |

---

## 6. 安全设计

### 6.1 JWT 认证

```java
// JwtUtil.java
private static final String SECRET = "typingquizsecretkey2024";
private static final long EXPIRATION = 86400000L; // 24小时
```

**Token 结构：**
- Payload: `userId:username`
- 有效期: 24小时
- 签名算法: HS256

---

### 6.2 账户数据隔离

所有数据查询必须按 `userId` 过滤：

```java
// Repository 层
List<Quiz> findByUserId(Long userId);
List<Quiz> findByUserIdWithAnswers(@Param("userId") Long userId);

// Service 层
public Quiz getQuizById(Long id, Long userId) {
    Quiz quiz = quizRepository.findByIdWithAnswers(id)...
    if (userId != null && !userId.equals(quiz.getUserId())) {
        throw new RuntimeException("无权访问此测验");
    }
    return quiz;
}
```

---

### 6.3 密码加密

使用 Spring Security BCrypt：

```java
private final BCryptPasswordEncoder passwordEncoder;

public Optional<User> login(String username, String password) {
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
        return user;
    }
    return Optional.empty();
}
```

---

## 7. 数据流设计

### 7.1 测验创建流程

```
Controller (POST /api/quizzes)
    ↓
Service.createQuiz(QuizDTO, userId)
    ↓
1. 验证输入
2. 创建 Quiz 实体
3. 绑定 userId (账户隔离)
4. 创建 Answer 实体列表
5. 处理分组关联
    ↓
Repository.save(quiz)
    ↓
返回 QuizResponseDTO
```

---

### 7.2 答案验证流程

```
用户输入 → 前端 → POST /api/answers/validate
    ↓
AnswerService.validate(quizId, input)
    ↓
1. 查询测验的所有答案
2. 标准化输入（小写）
3. 匹配 normalizedContent
4. 返回匹配结果
```

---

## 8. 数据库表结构

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (PK)         │
│ username        │
│ email           │
│ password        │
│ created_at      │
│ updated_at      │
└─────────────────┘
       │
       │ 1:N
       ↓
┌─────────────────┐
│      quiz       │
├─────────────────┤
│ id (PK)         │
│ title           │
│ description     │
│ time_limit      │
│ user_id         │
│ quiz_type       │
│ created_at      │
└─────────────────┘
       │
       │ 1:N
       ↓
┌─────────────────┐
│     answer      │
├─────────────────┤
│ id (PK)         │
│ quiz_id (FK)    │
│ content         │
│ normalized_content│
│ comment         │
└─────────────────┘

┌─────────────────┐
│   quiz_group    │
├─────────────────┤
│ id (PK)         │
│ name            │
│ user_id         │
│ display_order   │
└─────────────────┘
       │
       │ M:N
       ↓
┌─────────────────────┐
│   quiz_group_quiz   │ (中间表)
├─────────────────────┤
│ group_id (FK)       │
│ quiz_id (FK)        │
└─────────────────────┘
```

---

## 9. 配置管理

### 9.1 application.properties

```properties
# 服务器配置
server.port=8080

# MySQL 数据库配置
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/typing_quiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=typingquiz
spring.datasource.password=pianzigunsb123..

# JPA 配置
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
```

---

### 9.2 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MYSQL_HOST | localhost | MySQL 服务器地址 |
| 本地开发 | 47.102.147.127 | 连接云端数据库 |
| 云端部署 | localhost | 使用本地 MySQL |

---

## 10. 部署架构

```
┌─────────────────────────────────────────────────────┐
│                   前端 (浏览器)                      │
│              HTML/CSS/Vanilla JS                     │
└────────────────────┬────────────────────────────────┘
                     │ HTTP
                     ↓
┌─────────────────────────────────────────────────────┐
│              Spring Boot 应用 (8080)                 │
│  ┌─────────────┬─────────────┬────────────────────┐  │
│  │ Controller  │  Service    │  Repository        │  │
│  └─────────────┴─────────────┴────────────────────┘  │
└────────────────────┬────────────────────────────────┘
                     │ JDBC
                     ↓
┌─────────────────────────────────────────────────────┐
│              MySQL 8.0 (3306)                        │
└─────────────────────────────────────────────────────┘
```

**Docker 部署：**
```bash
docker build -t typing-quiz-app .
docker run -d --network host --name typing-quiz-app -v ./data:/app/data typing-quiz-app
```

---

## 11. 关键设计决策

| 决策 | 说明 |
|------|------|
| 分层架构 | Controller → Service → Repository → Entity |
| JPA 简化数据访问 | 使用 Spring Data JPA 自动生成 CRUD |
| 账户隔离 | 所有查询按 userId 过滤 |
| JWT 无状态认证 | 服务端无需存储 Session |
| BCrypt 密码加密 | 防彩虹表攻击 |
| 标准化答案匹配 | normalizedContent 实现大小写不敏感 |
| 级联删除 | 测验删除时自动删除关联答案 |
| CORS 开放 | 支持跨域请求 (`@CrossOrigin(origins = "*")`) |

---

## 12. 扩展性考虑

| 方向 | 当前实现 | 扩展建议 |
|------|----------|----------|
| 缓存 | 无 | 添加 Redis 缓存热点数据 |
| 分页 | 无 | Repository 添加 Page<> 返回 |
| 搜索 | 基础模糊查询 | 考虑 Elasticsearch |
| 消息队列 | 无 | 添加异步任务处理 |
| 监控 | 无 | 添加 Actuator 端点 |

---

**最后更新：** 2026-02-08
